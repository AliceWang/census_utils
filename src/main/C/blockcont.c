/*
* Contiguity list generator for Census shapefiles
* Copyright (C) <2009> <Joshua Justice, Sumanth Narendra, Alice Wang>
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
#include <math.h>
#include "shapefil.h"
#include "blockcont.h"
#define DBL_TOLERANCE 0.0000001 //TODO: get this exact value
#define TRUE 1
#define FALSE 0
#define QUEEN_TOLERANCE DBL_TOLERANCE

//UTList * neighbors; //TODO: malloc an array of x pointers to linked lists where x = block count
//TODO: the neighbor list  should be in the main code
// not in a lower level function (make it global)
/*
  neighborlist * neighbors=(neighbor*) malloc (sizeof(neighborlist)*block_count);
*/

int checkCont(SHPObject *a, SHPObject *b)
{
  int jLim;
  int hLim;
  int i,j,k,h;
  for(i=0;i<a->nParts;i++) //accounts for multiple polygons
    {
      if(i==a->nParts-1){
        jLim=a->nVertices-1;
      }
      else{
        jLim=a->panPartStart[i+1]-2;
      }
		
      for(j=a->panPartStart[i];j<jLim;j++)
        {
          for(k=0;k<b->nParts;k++){
            if(j==b->nParts-1){
              hLim=b->nVertices-1;
            }
            else{
              hLim=b->panPartStart[k+1]-2;
            }
            for(h=b->panPartStart[k];h<hLim;h++){
              if( checkOverlap(a->padfX[j], a->padfY[j], a->padfX[j+1],a->padfY[j+1],b->padfX[h], b->padfY[h], b->padfX[h+1],b->padfY[h+1])){
              //if(a->nShapeId == 20 || b->nShapeId == 20) {printf("\n 20 contig returning true for %d and %d \n", a->nShapeId, b->nShapeId); }
                return TRUE;
              }
            }
          }
        }
    }
     return FALSE;
}
int checkOverlap(double xa1, double ya1, double xa2, double ya2, double xb1, double yb1, double xb2, double yb2)
{
  double delXa=(xa2-xa1);          
  double delYa=(ya2-ya1);
  double delXb=(xb2-xb1);
  double delYb=(yb2-yb1);
  double ma=delYa/delXa;
  double mb=delYb/delXb;

  if((abs(ma-mb)>DBL_TOLERANCE)||(delXa==0&&delXb!=0)||(delXa!=0&&delXb==0)){
    return FALSE;
  }

  double xAmin=xa1;
  double xBmin=xb1;
  double xAmax=xa2;
  double xBmax=xb2;
  double yAmin=ya1;
  double yBmin=yb1;
  double yBmax=yb2;
  double yAmax=ya2;
  if(xa1>xa2)
    {
      xAmax=xa1;
      xAmin=xa2;
    }
  if(ya1>ya2)
    {
      yAmax=ya1;
      yAmin=ya2;
    }
  if(yb1>yb2)
    {
      yBmax=yb1;
      yBmin=yb2;
    }
  if(xb1>xb2)
    {
      xBmax=xb1;
      xBmin=xb2;
    }

  double ytest1=yb1-ya1;
  double ytest2=yb2-ya1;
  double ytest3=yb1-ya2;
  double mx1=ma*(xb1-xa1);
  double mx2=ma*(xb2-xa1);
  double mx3=ma*(xb1-xa2);

  if((xAmax<xBmin)||(yAmax<yBmin)){ // both x and y values are outside the values for the other block
    return FALSE;
  }		
  else if(delXa==0&&delXb==0)
    {
      if(xa1==xb1){
        return TRUE;
      }
    }
  else if ((ytest1-mx1)<=DBL_TOLERANCE&&(ytest2-mx2)<=DBL_TOLERANCE&&(ytest3-mx3)<=DBL_TOLERANCE){ // y=mx+b within tolerance
    return TRUE;
  }
  else{
    return FALSE;
  }
  return FALSE;
}

void callOverlapTestCode()
{
  int test1, test2, test3, test4, test5, test6;	


  test1 = checkOverlap(3, 3.5, 2, 3, 1, 2.5, 4, 4);  //should return true

  test2 = checkOverlap(1, 2.5, 2, 3, 3, 3.5, 4, 4);  //should return false

  test3 = checkOverlap(1,2.5,3, 3.5, 2, 3, 4, 4);    //should return true

  test4 = checkOverlap(4, 0, 10, 0, 4, 0, 10, 0);    //should return true

  test5 = checkOverlap(0,4,0,10,0,5,0,11);	//should return true

  test6 = checkOverlap(1,1,-1,-1,-1,1,1,-1);	//should return false

  printf("test1 should be true: %i\n",test1);
  printf("test2 should be false: %i\n",test2);
  printf("test3 should be true: %i\n",test3);
  printf("test4 should be true: %i\n",test4);
  printf("test5 should be true: %i\n",test5);
  printf("test6 should be false: %i\n",test6);



}

int queen_Contig(SHPObject *a, SHPObject *b)
{
  int i,j;
  for(i=0;i<a->nVertices;i++){
    for(j=0;j<b->nVertices;j++){
      if(fabs(a->padfX[i] - b->padfX[j]) < QUEEN_TOLERANCE
	 && fabs(a->padfY[i] - b->padfY[j] < QUEEN_TOLERANCE)){
	//printf("a = %f\t%f\tb=%f\t%f\n", a->padfX[i], a->padfY[i], b->padfX[j], b->padfY[j]);
	return TRUE;
      }
    }
  }	
  return FALSE;
}
