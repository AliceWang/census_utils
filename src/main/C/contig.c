/*
* Contiguity list generator for Census shapefiles
* Copyright (C) <2009> <Joshua Justice, Sumanth Narendra>
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*
* The Shapelib library is licensed under the GNU Lesser General Public License.
* A copy of the GNU LGPL can be found on http://www.gnu.org/licenses/lgpl-3.0.txt .
* For information on Shapelib, see http://shapelib.maptools.org/ .
* */
 
#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include "shapefil.h"
#include "uthash.h"
#include "blockcont.h"
#include "utlist.h"
 
#define TRUNCATE_UPTO 	100
#define TOLERANCE 	0.05
 
#define TRUNCATE(var, convert) { \
int temp = var*TRUNCATE_UPTO; \
convert = (double)temp/TRUNCATE_UPTO; \
}
 
#define ADD_TO_BUCKET_LIST(present, traverse, new_entry, object) { \
new_entry = malloc(sizeof(bucket_list)); \
new_entry->block = object; \
new_entry->next_block = NULL; \
traverse = (bucket_list *)present->head_of_list; \
while( traverse->next_block != NULL) \
traverse = (bucket_list *)traverse->next_block; \
traverse->next_block = (struct bucket_list *)new_entry; \
}
 
#define ADD_TO_HASH_TABLE(new_bucket, padfX, padfY, new_entry, x_inc, y_inc) { \
new_bucket = malloc(sizeof(HT_Struct_For_Block)); \
memset(new_bucket, 0, sizeof(HT_Struct_For_Block)); \
new_entry = malloc(sizeof(bucket_list)); \
new_entry->block = object; \
new_entry->next_block = NULL; \
new_bucket->head_of_list = new_entry; \
new_bucket->padfX = padfX + x_inc; \
new_bucket->padfY = padfY + y_inc; \
HASH_ADD(hh, HT_Blocks, padfX, keylen, new_bucket); \
}
 
/* The key for the hash table for blocks is the product of the x and y coordinates of the first vertix of the outermost ring of
* the block. This is done so that all the blocks with a common point would be in the common bucket */
 
typedef struct
{
    double padfX;
    double padfY;
}lookup_key;
 
typedef struct
{
    SHPObject *block;
    struct bucket_list *next_block;
}bucket_list;
 
typedef struct
{
    double padfX; /*padfX and padfY comprise the key. Both are passed onto the hash ADD */
    double padfY;
    bucket_list *head_of_list;
    UT_hash_handle hh;
} HT_Struct_For_Block;
 
struct neighbor_list
{
    int index; //Store the index of the neighbor block. This is used later to sort
    struct neighbor_list *next;
    struct neighbor_list *prev; //dummy variable only for utlist sort to work.
};
 
/*void int_to_string(int num, char *ptr)
{
sprintf(retstr, "%i", num);
}*/
 
HT_Struct_For_Block *HT_Blocks = NULL;
SHPObject **block_list = NULL;
int block_count;
//char sf_name[] = "/home/sumanth/Documents/eDemocracy/Files/Fultoncombinednd.shp";
//char sf_name[] = "/home/joshua/FultonCoData/Fultoncombinednd.shp";
char sf_name[] = "/home/altheacynara/Documents/saltLakeData/tl_2008_49035_tabblock.shp";
//char sf_name[]="/home/altheacynara/Documents/fultonData/Fultoncombinednd.shp";
int **Sub_Graph_Head = NULL;
int sub_graph_count = 0;
int *visited = NULL, *centroid_list_SG;
 
struct neighbor_list **NTABLE = NULL;
 
void Add_block_to_HT();
 
void Add_Blocks_to_HT(SHPHandle handle)
{
  int i;
  SHPGetInfo(handle, &block_count, NULL, NULL, NULL);
  printf("\nTotal number of blocks identified from SHPGetInfo = %d", block_count);
  block_list = malloc(block_count*sizeof(SHPObject *));
  for(i=0; i<block_count; i++)
  {
  block_list[i] = SHPReadObject(handle, i);
  Add_block_to_HT(block_list[i]);
  //printf("\n added %f %f", block_list[i]->padfX[0], block_list[i]->padfY[0]);
  }
 
  /* This code was used for testing. Believe it is better to have this for debugging purposes in the future.
for(i=0; i<block_count; i++)
{
HT_Struct_For_Block *test2 = NULL;
double padfX, padfY;
int count = 0, keylen;
lookup_key *key1 = malloc(sizeof(lookup_key));
TRUNCATE(block_list[i]->padfX[0], padfX);
TRUNCATE(block_list[i]->padfY[0], padfY);
while (count < 4)
{
memset(key1, 0, sizeof(*key1));
key1->padfX = padfX; key1->padfY = padfY;
test2 = NULL;
if(count == 1) { key1->padfX += TOLERANCE; }
else if(count == 2) { key1->padfY += TOLERANCE; }
else if(count == 3) { key1->padfX += TOLERANCE; key1->padfY += TOLERANCE; }
keylen = offsetof(HT_Struct_For_Block, padfX) + sizeof(double) - offsetof(HT_Struct_For_Block, padfY);
printf("\n to be found!! %f %f", key1->padfX, key1->padfY);
HASH_FIND(hh, HT_Blocks, &key1->padfX, keylen, test2);
if(test2 != NULL)
printf("\n found!!");
else
printf("\n not found!!\n");
count++;
}
free(key1);
}*/
}
 
/* check_for_entry_in_HT checks if there is an entry already present in the HT with the same key. (hash collision).
* If there is a hash collision we add it to the bucket_list */
 
HT_Struct_For_Block * check_for_entry_in_HT(double padfX, double padfY)
{
  HT_Struct_For_Block *present = NULL;
  int keylen = offsetof(HT_Struct_For_Block, padfY) + sizeof(double) - offsetof(HT_Struct_For_Block, padfX);
 
  lookup_key *key = malloc(sizeof(lookup_key));
  memset(key, 0, sizeof(*key));
  key->padfX = padfX;
  key->padfY = padfY;
 
  HASH_FIND(hh, HT_Blocks, &key->padfX, keylen, present);
  free(key);
  if(present != NULL)
      return present;
 
  return NULL;
}
 
void Add_block_to_HT(SHPObject *object)
{
  HT_Struct_For_Block *new_block1, *new_block2, *new_block3, *new_block4, *present;
  double padfX, padfY;
  int keylen = offsetof(HT_Struct_For_Block, padfY) + sizeof(double) - offsetof(HT_Struct_For_Block, padfX);
  TRUNCATE(object->padfX[0], padfX);
  TRUNCATE(object->padfY[0], padfY);
  bucket_list *traverse, *new_entry;
  /*new_entry = malloc(sizeof(struct bucket_list *));
  memset(new_entry, 0, sizeof(struct bucket_list *)); 
  new_entry->block = object;
  new_entry->next_block = NULL;*/
 
  if ((present = check_for_entry_in_HT(padfX, padfY)) != NULL) { //add to the bucket_list
     //printf("\n already in HT 1 %f %f", padfX, padfY);
     ADD_TO_BUCKET_LIST(present, traverse, new_entry, object); }
  else{
     ADD_TO_HASH_TABLE(new_block1, padfX, padfY, new_entry, 0, 0);
     //printf("\n not present..adding.. 1 %f %f\n", padfX, padfY);
     }
  
  if ((present = check_for_entry_in_HT(padfX + TOLERANCE, padfY)) != NULL) { //add to the bucket_list
     //printf("\n already in HT 2");
     ADD_TO_BUCKET_LIST(present, traverse, new_entry, object); }
  else{
     ADD_TO_HASH_TABLE(new_block2, padfX, padfY, new_entry, TOLERANCE, 0);
     //printf("\n not present..adding.. 2\n");
     }
  
  if ((present = check_for_entry_in_HT(padfX, padfY + TOLERANCE)) != NULL) { //add to the bucket_list
     //printf("\n already in HT 3");
     ADD_TO_BUCKET_LIST(present, traverse, new_entry, object); }
  else{
     ADD_TO_HASH_TABLE(new_block3, padfX, padfY, new_entry, 0, TOLERANCE);
     //printf("\n not present..adding.. 3\n");
     }
  
  if ((present = check_for_entry_in_HT(padfX + TOLERANCE, padfY + TOLERANCE)) != NULL) { //add to the bucket_list
     //printf("\n already in HT 4");
     ADD_TO_BUCKET_LIST(present, traverse, new_entry, object); }
  else{
     ADD_TO_HASH_TABLE(new_block4, padfX, padfY, new_entry, TOLERANCE, TOLERANCE);
     //printf("\n not present..adding.. 4\n");
     }
}
  
void print_table()
{
  HT_Struct_For_Block *s;
  bucket_list *temp = NULL;
  printf("\n printing elements in the hash table \n");
  for(s=HT_Blocks; s != NULL; s=s->hh.next)
  {
    //temp = (bucket_list *)s->head_of_list.next_block;
    temp = (bucket_list *)s->head_of_list;
    printf("\nfor padfX = %f, padfY = %f \n", s->padfX, s->padfY);
    while(temp != NULL)
    {
       //printf("\nin the bucket_list");
       printf("%d\t", temp->block->nShapeId);
       temp = (bucket_list *)temp->next_block;
    }
  }
}
 
void Add_to_NList(int first, int second)
{
  struct neighbor_list *traverse;
  struct neighbor_list *neighbor_node = malloc(sizeof(struct neighbor_list *));
  neighbor_node->index = second;
  neighbor_node->next = neighbor_node->prev = NULL;
  if (NTABLE[first] == NULL)
  {
     NTABLE[first] = neighbor_node;
  }
  else
  {
     traverse = NTABLE[first];
     while(traverse->next != NULL)
     {
          if (traverse->index == second)
          {
              return;
          }
          traverse = traverse->next;
     }
     if (traverse->index == second)
     {
          return;
     }
         
     traverse->next = neighbor_node;
  }
}
 
void generate_neighbor_table()
{
  HT_Struct_For_Block *s;
  bucket_list *temp = NULL, *temp_next = NULL;
 
  NTABLE = malloc(block_count * sizeof(struct neighbor_list *));
 
  //Go over the HT; for every pair of objects (a,b) in the bucket_list add (a,b) and (b,a) to the neighbor list
  for (s=HT_Blocks; s != NULL; s=s->hh.next)
  {
     //temp = (bucket_list *)s->head_of_list.next_block;
     temp = (bucket_list *)s->head_of_list;
     while(temp !=NULL)
     {
        temp_next = (bucket_list *)temp->next_block;
        while(temp_next != NULL)
	  {
	    if(checkCont(temp->block, temp_next->block))
	      {
		if(queen_Contig(temp->block, temp_next->block))
		  {
		    Add_to_NList(temp->block->nShapeId, temp_next->block->nShapeId);
		    Add_to_NList(temp_next->block->nShapeId, temp->block->nShapeId);
		  }
	      }
            temp_next = (bucket_list *)temp_next->next_block;
	  }
        temp = (bucket_list *)temp->next_block;
      }
  }
}
 
int sort_algo(struct neighbor_list *first, struct neighbor_list *second)
{
  if (first->index < second->index) return -1;
  else if (first->index == second->index) return 0;
  else return 1;
}
 
void sort_NTABLE()
{
  int i;
  for (i=0; i<block_count; i++)
    LL_SORT(NTABLE[i], sort_algo);
}
 
void print_neighbor_table()
{
  struct neighbor_list *temp = NULL;
  int i;
  for (i=0;i<block_count; i++)
  {
     temp = NTABLE[i];
     printf("\n neighbor list of %d: ", i);
     while (temp != NULL)
     {
        printf("%d\t", temp->index);
        temp = temp->next;
     }
  }
}
 
void Output_To_GAL()
{
  int i;
  char *str_bc = calloc(100, sizeof(char));
  char *ego_label = calloc(100, sizeof(char));
  char *n_count = calloc(100, sizeof(char));
  char *n_list = calloc(10000, sizeof(char)); /*having three char ptrs will make writing into the .gal file faster */
  char *temp_str = calloc(100, sizeof(char));
  struct neighbor_list *temp;
  int fn_len = strlen(sf_name);
  char gal_filename[fn_len];
  strcpy(gal_filename, sf_name);
  gal_filename[fn_len-3] = 'G';
  gal_filename[fn_len-2] = 'A';
  gal_filename[fn_len-1] = 'L';
  FILE *fp = fopen(gal_filename, "w+");
  sprintf(str_bc, "%i", block_count);
  strcat(str_bc, "\n");
  fputs(str_bc, fp);
  for(i=0;i<block_count;i++)
  {
    int num_neighbors = 0;
    sprintf(ego_label, "%i", i+1);
    strcat(ego_label, " ");
 
    /* We do not know the neighbor count of a vertex here; have to iterate through every
* neighbor list to find the count. Instead of iterating twice to find the count and
* again to print them we have separate lists for neibhbor count and neighbor list.
* */
 
    temp = NTABLE[i];
    while(temp != NULL)
    {
      num_neighbors++;
      sprintf(temp_str, "%i \0", (temp->index)+1);
      strcat(n_list, temp_str);
      temp = temp->next;
    }
    fputs(ego_label, fp);
    sprintf(n_count, "%i", num_neighbors);
    strcat(n_count,"\n\0");
    fputs(n_count, fp);
    //strcat(n_list,"\n\n");
    strcat(n_list,"\n");
    fputs(n_list, fp);
    bzero(n_list, sizeof(n_list));
  }
  fclose(fp);
  free(str_bc);
  free(ego_label);
  free(n_count);
  free(n_list);
  free(temp_str);
  return;
}

/* Checks to see if the block is already inserted in the SG.
 * If it is already inserted, returns 0.
 */

int not_inserted_in_SG(int *SG_head, int block_no)
{
  int *temp = SG_head;

  while(*temp != -1)
  {
    if(*temp == block_no)
       return 0;
    temp++;
  }

  return 1;
}

/* SG_head: head of the subgraph
 * head: node whose neighbor table has to be traversed
 * insert_here: position in the array where the next neighbor has to be inserted
 * block_no: block id of the head 
 */

void add_to_sub_graph(int *SG_head, int *head, int *insert_here, int block_no)
{
  struct neighbor_list *temp = NULL; 

  if(visited[block_no] != 1)
  {
    visited[block_no] = 1;

    /* populate with the neighbors of block_no */
    temp = NTABLE[block_no];
    while(temp!=NULL)
    {
       if(!visited[temp->index] && not_inserted_in_SG(SG_head, temp->index))
       { 
         *insert_here = temp->index;
         insert_here++;
	 *insert_here = -1;
       }
       temp = temp->next;
    }
  }
  head++;
  if(*head != -1)
  {
    //printf("\n calling add_to_sub_graph with value %d", *head);
    add_to_sub_graph(SG_head, head, insert_here, *head);
  }

  return;
}

/* compute_Sub_Graphs computes the subgraphs which are disconnected.
 * It returns the number of subgraphs.
 * Have array included_Blocks[blockCount]. Initialize all of them to 0.
 * Do a breadth first on each node in the neibhbor list. If the node is not already included in any subgraph add it.
 */

void compute_Sub_Graphs()
{
  int i, j;
  Sub_Graph_Head = malloc(block_count * sizeof(int *));
  int included_Blocks[block_count];
  bzero(included_Blocks, block_count);
  visited = malloc(block_count * sizeof(int));

  for(i=0;i<block_count;i++) 
  {
    /*do not traverse the block if it already traversed*/
    if(visited[i] == 1)
    {
      //printf("\n %d already traversed \n", i);
    }
    else
    {
      int *head, *next;
      Sub_Graph_Head[sub_graph_count] = malloc(block_count * sizeof(int));
      head = Sub_Graph_Head[sub_graph_count];
      bzero(head, block_count);
      head[0] = i; /* the first element of the subgraph is the block itself*/
      head[1] = -1; /* to identify the end of the array */
      next = head + 1;
      add_to_sub_graph(head, head, next, i);
      sub_graph_count++;
    }
  }

  printf("\n Printing Subgraphs:\nNumber of subgraphs: %d\n", sub_graph_count);
  for(i=0;i<sub_graph_count;i++)
  {
     printf("Subgraph %d:\n", i);
     for(j=0;Sub_Graph_Head[i][j]!=-1;j++)
	printf("%d\t", Sub_Graph_Head[i][j]);
     printf("\n");
  }

  return;
}

void Join_Sub_Graphs()
{
  /* compute the centroids of each SG.
 */

  int i, j, temp;
  int min_x, max_x, min_y, max_y;

  printf("\n Join_Sub_Graphs");
  centroid_list_SG = malloc(sub_graph_count * sizeof(int));
  for(i=0; i<sub_graph_count; i++)
  {
    min_x = max_x = min_y = max_y = Sub_Graph_Head[i][0];
    for(j=1; Sub_Graph_Head[i][j] != -1; j++)
    {
      temp = Sub_Graph_Head[i][j];
      //TODO: compute the centroid of each block and update min_x, min_y, max_x, max_y accordingly.
    }
  }
}
 
//This function can be called to test on the hash table
void test_hashing()
{
  HT_Struct_For_Block *test1, *test2 = NULL;
  int keylen = offsetof(HT_Struct_For_Block, padfY) + sizeof(double) - offsetof(HT_Struct_For_Block, padfX);
  double padfX1 = 37.45;
  double padfY1 = 234.54;
 
  test1 = malloc(sizeof(HT_Struct_For_Block));
  memset(test1 , 0, sizeof(HT_Struct_For_Block));
  test1->padfX = padfX1;
  test1->padfY = padfY1;
  HASH_ADD(hh, HT_Blocks, padfX, keylen, test1);
  print_table();
 
  lookup_key *key1 = malloc(sizeof(lookup_key));
  memset(key1, 0, sizeof(*key1));
  TRUNCATE(-84.383048, key1->padfX);
  TRUNCATE(33.786532, key1->padfY);
  printf("\n trying to find %f %f\n", key1->padfX, key1->padfY);
  HASH_FIND(hh, HT_Blocks, &key1->padfX, keylen, test2);
  
  if(test2 != NULL)
     printf("\n found!! %f %f\n", test2->padfX, test2->padfY);
  else
     printf("\n not found!!\n");
  free(test1);
  free(key1);
}
 
int main(){
  int i;
  /* handle has to be pointed to the right location */
  SHPHandle handle = SHPOpen(sf_name, "rb");
  Add_Blocks_to_HT(handle);
 
  printf("\nTotal number of slots in the block HT = %d\n", HASH_COUNT(HT_Blocks));
  //test_hashing();
  //print_table();
 
  generate_neighbor_table();
 
  sort_NTABLE();
  //compute_Sub_Graphs();
  //Join_Sub_Graphs();
 
  /* Use print_neighbor_table to print the neighbors of all the blocks */
  //print_neighbor_table();
  Output_To_GAL();
 
  //free all the items in the HT
  HT_Struct_For_Block *current;
  bucket_list *temp = NULL, *temp_next = NULL;
  while (HT_Blocks)
  {
     current = HT_Blocks;

     /* Leave the first block in the bucket and start deleting
 * from the second block. The first block is deleted when doing
 * the HASH_DEL
 * */

     temp = (bucket_list *)current->head_of_list->next_block;
     /* delete the bucket_list too if present */
     while(temp != NULL)
     {
         temp_next = (bucket_list *)temp->next_block;
         free(temp);
         temp = temp_next;
     }
     HASH_DEL(HT_Blocks, current);
     free(current);
  }
 
  for(i=0; i<block_count; i++)
  SHPDestroyObject(block_list[i]);
 
  //free the NTABLE
  struct neighbor_list *traverse, *traverse_next;
  for(i=0; i<block_count; i++)
  {
    traverse = traverse_next = NTABLE[i];
    while (traverse != NULL)
    {
      traverse_next = traverse->next;
      free(traverse);
      traverse = traverse_next;
    }
  }
  free(NTABLE);
  NTABLE = NULL;
  return 0;
}
