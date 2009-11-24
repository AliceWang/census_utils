#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "shapefil.h"
#include "neighbors.h"
#define SVG_SCALE 5000
/*
  Code to display Census shapefiles.
  Copyright (C) <2009>  <Joshua Justice>

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.,
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
/*
  The Shapelib library is licensed under the GNU Lesser General Public License.
  A copy of the GNU LGPL can be found on http://www.gnu.org/licenses/lgpl-3.0.txt .
  For information on Shapelib, see http://shapelib.maptools.org/ .
*/
/*
  EULA: The Graphics Gems code is copyright-protected. 
  In other words, you cannot claim the text of the code as your own and resell it. 
  Using the code is permitted in any program, product, or library, non-commercial or commercial. 
  Giving credit is not required, though is a nice gesture. 
  The code comes as-is, and if there are any flaws or problems with any Gems code, nobody involved
  with Gems - authors, editors, publishers, or webmasters - are to be held responsible. 
  Basically, don't be a jerk, and remember that anything free comes with no guarantee. 
*/



/*  
    polyCentroid: Calculates the centroid (xCentroid, yCentroid) and area
    of a polygon, given its vertices (x[0], y[0]) ... (x[n-1], y[n-1]). It
    is assumed that the contour is closed, i.e., that the vertex following
    (x[n-1], y[n-1]) is (x[0], y[0]).  The algebraic sign of the area is
    positive for counterclockwise ordering of vertices in x-y plane;
    otherwise negative.

    Returned values:  0 for normal execution;  1 if the polygon is
    degenerate (number of vertices < 3);  and 2 if area = 0 (and the
    centroid is undefined).
*/

int polyCentroid(double x[], double y[], int n,
		 double *xCentroid, double *yCentroid, double *area){
  register int i, j;
  double ai, atmp = 0, xtmp = 0, ytmp = 0;
  if (n < 3) return 1;
  for (i = n-1, j = 0; j < n; i = j, j++){
    ai = x[i] * y[j] - x[j] * y[i];
    atmp += ai;
    xtmp += (x[j] + x[i]) * ai;
    ytmp += (y[j] + y[i]) * ai;
  }
  *area = atmp / 2;
  if (atmp != 0){
    *xCentroid =	xtmp / (3 * atmp);
    *yCentroid =	ytmp / (3 * atmp);
    return 0;
  }
  return 2;
} //end Graphics Gems code

void colorArrange(int* array, int n, int nDists, char *distFile){
  unsigned int distarray_size = nDists+1;
  unsigned int *distArray = malloc((nDists+1)*sizeof(unsigned int));
  FILE *fp;
  int blockno;
  int distno;
  fp = fopen(distFile, "r");
  if(fp==NULL){
    printf("Error- could not open district file\n");
    printf("Filename:  %s\n", distFile);	
  }
  //Sumanth - Debug
  unsigned int min=0xffffff;
  unsigned int max=0x000000;
  unsigned int diff=(min-max)/distarray_size;
printf("min=%x max=%x distarray_size=%x diff=%x  ",min, max,distarray_size, diff);
  int i;
  //  int arrayLim;
  unsigned int current = max;
  //check array size here
  for(i=0; i<distarray_size; i++){
printf("current=%x  ", current);
    distArray[i]=current;
    current=current+diff;
  }
  while(fscanf(fp, "%i %i", &blockno, &distno) != EOF){
    array[blockno]=distArray[distno];
    printf("%xi   ",distArray[distno]);
  }
  free(distArray);
  fclose(fp);
}

void svg_header(FILE *svg){
  fputs("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n", svg);
  fputs("<svg\n\txmlns:svg=\"http://www.w3.org/2000/svg\"\n", svg);
  fputs("\txmlns=\"http://www.w3.org/2000/svg\"\n", svg);
  fputs("\tversion=\"1.0\"\n", svg);
  fputs("\twidth=\"360000\"\n", svg);
  fputs("\theight=\"180000\"\n", svg);
  fputs("\tid=\"svg2\">\n", svg);
  fputs("\t<defs\n\t\tid=\"defs1\" />\n", svg);
  fputs("\t<g\n\t\tid=\"layer1\">\n", svg);
  return;
}

void svg_polygon(SHPObject block, FILE *svg, int use_dist, int* colorArray){
  int i,j,jLim;
  double x,y;
  fputs("\t\t<path\n\t\t\td=\"", svg);  
  for(i=0;i<block.nParts;i++){
    if(i==block.nParts-1){
      jLim=block.nVertices-1;
    }else{
      jLim=block.panPartStart[i+1]-2;
    }
    for(j=block.panPartStart[i];j<jLim;j++){
      //draw coordinates at padfX[j] etc.
      if(j==block.panPartStart[i]){
	fputs("M ",svg); //not having the \n is deliberate
      }else{
	fputs("L ",svg); //no \n is also deliberate here
      }
      x=(block.padfX[j]+180)*SVG_SCALE;
      y=(block.padfY[j]-90)*-SVG_SCALE; //SVG has y-down
      fprintf(svg, "%f %f ",x,y);
    }
  }
  fprintf(svg,"\"\n\t\t\tid=\"path%d\"\n",block.nShapeId);
  if(use_dist){
    fprintf(svg,"\t\t\tstyle=\"fill:#%x;fill-rule:evenodd;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1\"/>", colorArray[block.nShapeId]);
  }else{
    fprintf(svg,"\t\t\tstyle=\"fill:#%x;fill-rule:evenodd;stroke:#000000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1\"/>", colorArray[block.nShapeId]);
  }
  return;
}

void svg_neighbors(SHPObject block, struct neighbor_list neighbor_list,
                   double xCentList[], double yCentList[], FILE *svg){
  //TODO: write this function
  //The process is as follows:
  //for each neighbor to the block, print the path between the centroids
    
  //Sumanth - Debug
  printf("\n blockid = %d \n", block.nShapeId);
 
  int current, i;
  double bx, by, nx, ny;
  int ncount = neighbor_list.num_neighbors;
  //Sumanth - Debug
  //bx = xCentList[block.nShapeId - 1];
  //by = yCentList[block.nShapeId - 1];
  bx = xCentList[block.nShapeId];
  by = yCentList[block.nShapeId];
  bx = (bx+180)*SVG_SCALE;
  by = (by-90)*-SVG_SCALE;

  for(i=0; i<ncount; i++){
    current = neighbor_list.neighbors[i];
    nx = xCentList[current];
    ny = yCentList[current];
    nx = (nx+180)*SVG_SCALE;
    ny = (ny-90)*-SVG_SCALE; //need to match scale with the other code!
    //draw paths here 
    fputs("\t\t<path\n\t\t\td=\"", svg);
    fprintf(svg, "M %f %f ",bx, by); //Moveto block X/Y 
    fprintf(svg, "L %f %f\"\n", nx, ny); //Lineto neighbor X/Y
    fprintf(svg,"\n\t\t\tid=\"path%d\"\n",block.nShapeId); //id
    fprintf(svg,"\t\t\tstyle=\"fill:#ffffff;fill-rule:evenodd;stroke:#ff0000;stroke-width:1px;stroke-linecap:butt;stroke-linejoin:miter;stroke-opacity:1\"/>\n"); //style
  }

  return;
}

void svg_footer(FILE *svg){
  fputs("\t</g>\n", svg);
  fputs("</svg>", svg);
}

int main(){
  int entityCount;
  int shapeType;
  double padfMinBound[4];
  double padfMaxBound[4];
  int i;
  int use_gal = 0;
  int use_dist = 1;
  //For josh
  //char sf_name[] = "/home/josh/Desktop/FultonCoData/Fultoncombinednd.shp";
  //for sumanth
  //char sf_name[] = "/home/sumanth/Documents/eDemocracy/Files/Fultoncombinednd.shp";
  //for alice
  char sf_name[]= "/home/altheacynara/Documents/fultonData/Fultoncombinednd.shp";
  //Eventually, this won't be hardcoded

  SHPHandle handle = SHPOpen(sf_name, "rb");


  int fn_len = strlen(sf_name);
  char svg_filename[fn_len];
  char gal_filename[fn_len];
  char dst_filename[fn_len];
  FILE *svg;
  strcpy(svg_filename, sf_name);
  strcpy(gal_filename, sf_name);
  strcpy(dst_filename, sf_name);
  svg_filename[fn_len-2] = 'v';
  svg_filename[fn_len-1] = 'g';
  gal_filename[fn_len-3] = 'G';
  gal_filename[fn_len-2] = 'A';
  gal_filename[fn_len-1] = 'L';
  dst_filename[fn_len-3] = 'd';
  dst_filename[fn_len-2] = 's';
  dst_filename[fn_len-1] = 't';
  //I know, the above isn't really robust enough.
  //Should be improved upon when the file name is no longer hardcoded

  SHPGetInfo(handle, &entityCount, &shapeType, padfMinBound, padfMaxBound);
 
  SHPObject **shapeList = malloc(entityCount*sizeof(SHPObject *));
  //neighborList neighbors[entityCount];
  struct neighbor_list *NLIST;
  double xCentList[entityCount];
  double yCentList[entityCount];
  double areaList[entityCount];
  //populate the shapeList
  for(i=0; i<entityCount; i++){
    shapeList[i] = SHPReadObject(handle,i);
  }
  printf("Shapelist populated.\n");
  //delete file if it exists
  remove(svg_filename);
  //set up the SVG file pointer
  svg = fopen(svg_filename, "a+");
  printf("SVG file opened for writing.\n");
  //write header
  svg_header(svg);
  printf("SVG header printed.\n");
  //Call colorArrange:
  int ndists=5;
  int *colorArray = malloc(entityCount*sizeof(int));
  printf("The filename is %s\n compared to %s\n", dst_filename, gal_filename);
  colorArrange(colorArray,entityCount,ndists, dst_filename);

  //write individual polygons
  for(i=0; i<entityCount; i++){
    svg_polygon(*shapeList[i], svg, use_dist, colorArray);
  }
  printf("Polygons all printed.\n");
  if(use_gal){
    FILE *gal;
    int block, num_neigh, nblocks, temp_neigh;
    int count=0;
    printf("The name of the file is: %s\n", gal_filename);
    gal= fopen(gal_filename, "r");
    if(gal==NULL){
      printf("Error: Could not open GAL file.\n");
      return -1;
    }
    fscanf(gal, "%d", &nblocks);
    printf("GAL block count matches shapefile block count. Proceeding...\n");
	  
    NLIST = malloc(nblocks * sizeof(struct neighbor_list));
	  
    while(fscanf(gal, "%d %d", &block, &num_neigh) != EOF)
      {
	NLIST[block].num_neighbors = num_neigh;
	if(num_neigh != 0)
	  { 
	    count=0;
	    NLIST[block].neighbors = malloc(num_neigh * sizeof(int));
	    while(count < num_neigh)
	      {
		fscanf(gal, "%d", &temp_neigh);
		NLIST[block].neighbors[count] = temp_neigh;
		count++;
	      }
	  }
      }

    //Debugging: print the neighbor list of all blocks
    /*int i,j;
      for(i=0;i<nblocks;i++)
      {
      printf("%d %d\n", i, NLIST[i].num_neighbors);
      if(NLIST[i].num_neighbors > 0)
      for(j=0;j<NLIST[i].num_neighbors;j++)
      printf("%d ", NLIST[i].neighbors[j]);
      printf("\n");
      }*/

    //find centroids for every block
    for(i=0; i<entityCount; i++){
      int lastPoint;
      int status;
      SHPObject block = *shapeList[i];
      //Note that we're going to disregard holes, etc.
      if(block.nParts>1){
	lastPoint = block.panPartStart[1]-1;
      }else{
	lastPoint = block.nVertices-1;
      }
      status = polyCentroid(block.padfX, block.padfY, lastPoint, 
			    xCentList+i, yCentList+i, areaList+i);
    }
    printf("Centroids calculated.\n");
    //write paths from centroid to centroid
    fputs("\t</g>\n", svg);
    fputs("\t<g\n\t\tid=\"layer2\">\n", svg);
    for(i=0; i<entityCount; i++){
               
      svg_neighbors(*shapeList[i], NLIST[i], xCentList, yCentList, svg);
    }
    //svg_neighbors(*shapeList[20], NLIST[20], xCentList, yCentList, svg);
    printf("Contiguity paths drawn.\n");
       
    //Free NLIST
    for(i=0; i<entityCount; i++)
      {
	free(NLIST[i].neighbors);
	NLIST[i].neighbors = NULL;
      }
    free(NLIST);
    NLIST = NULL;

    fclose(gal);
  }
  
  //write footer
  svg_footer(svg);
  printf("SVG footer printed.\n");
  for(i=0; i<entityCount; i++){
    SHPDestroyObject(shapeList[i]);
  }
  SHPClose(handle);
  fclose(svg);
  free(colorArray);
  return 0;
}



//for testing
/*
  int main(){
  int ndists=10;
  int color_array[ndists+1];
  int i;
  colorArrange(color_array,ndists);
  for(i=0;i<=ndists;i++){
  printf("%X\n",color_array[i]);
  }
  return 0;
  }
*/

