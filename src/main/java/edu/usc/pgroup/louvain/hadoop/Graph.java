/*
 *  Copyright 2013 University of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.package edu.usc.goffish.gopher.sample;
 */
package edu.usc.pgroup.louvain.hadoop;

import org.apache.hadoop.mapred.FileInputFormat;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by Charith Wickramaarachchi on 6/25/14.
 */
public class Graph {


    public static final  int WEIGHTED  = 0;
    public static final int UNWEIGHTED  = 1;



    private int nb_nodes;
    private long nb_links;
    private double total_weight;

    private List<Long> degrees = new ArrayList<Long>(1000);
    private List<Integer> links = new ArrayList<Integer>(5000);
    private List<Float> weights = new ArrayList<Float>(5000);



    Graph(){
        nb_nodes     = 0;
        nb_links     = 0;
        total_weight = 0;

    }



    Graph(InputStream inputStream, int type) throws Exception{
        // Assume metis undirected format and unweighted graph.

        BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream));

        String line = fileReader.readLine();


        int nodeId = 0;
        while (line != null) {
            StringTokenizer tokenizer = new StringTokenizer(line);
            int count = 0;
            while (tokenizer.hasMoreTokens()) {
                int e = Integer.parseInt(tokenizer.nextToken());
                links.add(e);
                count++;
            }

            if(nodeId == 0) {
                degrees.add(nodeId,(long)count);
            } else {
                degrees.add(nodeId,(long)(degrees.get(nodeId -1) + count));
            }

            nodeId++;
            line = fileReader.readLine();
        }
        this.nb_nodes = nodeId;
        this.nb_links = degrees.get(nb_nodes -1);
        this.total_weight=0;

        // Compute total weight
        for (int i=0 ; i<nb_nodes ; i++) {
            total_weight += (double)weighted_degree(i);
        }

    }

    Graph(String fileName,int type) throws Exception {

        this(new FileInputStream(fileName), type);

    }



    void display() {
        for ( int node=0 ; node<nb_nodes ; node++) {
            Pair<Integer, Integer> p = neighbors(node);
            System.out.print("" + node + ":");

            for (int i=0 ; i<nb_neighbors(node) ; i++) {
                if (true) {
                    if (weights.size()!=0)
                        System.out.print(" (" + links.get(p.getElement0() + i) + " " + weights.get(p.getElement1() + i) + ")");
                    else
                        System.out.print(" " + links.get(p.getElement0() + i));
                }
            }
            System.out.print("\n");
        }
    }


    void display_binary(OutputStream out){
        //TODO
    }


    boolean check_symmetry() {
        int error=0;
        for (int node=0 ; node<nb_nodes ; node++) {
            Pair<Integer, Integer> p = neighbors(node);
            for (int i=0 ; i<nb_neighbors(node) ; i++) {
                 int neigh = links.get(p.getElement0() + i);
                float weight = weights.get(p.getElement1() + i);

                Pair<Integer, Integer> p_neigh = neighbors(neigh);
                for (int j=0 ; j<nb_neighbors(neigh) ; j++) {
                    int neigh_neigh = links.get(p_neigh.getElement0() + j);
                    float neigh_weight = weights.get(p_neigh.getElement1() + j);

                    if (node==neigh_neigh && weight!=neigh_weight) {
                        System.out.println(""+ node + " " + neigh + " " + weight + " " + neigh_neigh);
                        if (error++==10) {
                            System.exit(0);
                        }
                    }
                }
            }
        }
        return (error==0);
    }


    // return the number of neighbors (degree) of the node
    public long nb_neighbors(int node) {
        assert(node>=0 && node<nb_nodes);

        if (node==0)
            return degrees.get(0);
        else
            return (degrees.get(node)-degrees.get(node-1));

    }

    // return the number of self loops of the node
    public double nb_selfloops(int node){
        assert(node>=0 && node<nb_nodes);

        Pair<Integer, Integer> p = neighbors(node);
        for (int i=0 ; i < nb_neighbors(node) ; i++) {
            if (links.get(p.getElement0() + i) ==node) {
                if (weights.size()!=0)
                    return (double)weights.get(p.getElement1()+ i);
                else
                return 1.0;
            }
        }
        return 0.0;
    }

    // return the weighted degree of the node
    public double weighted_degree(int node){
        assert(node>=0 && node<nb_nodes);

        if (weights.size()==0)
            return (double)nb_neighbors(node);
        else {
            Pair<Integer, Integer> p = neighbors(node);
            double res = 0;
            for ( int i=0 ; i<nb_neighbors(node) ; i++) {
                res += (double)weights.get(p.getElement1()+i);
            }
            return res;
        }
    }

    // return pointers to the first neighbor and first weight of the node
    public Pair<Integer, Integer> neighbors(int node){
        if (node==0)
            return new Pair<Integer, Integer>(0,0);
        else if (weights.size()!=0)
            return new Pair(degrees.get(node-1),degrees.get(node-1));
        else
            return new Pair(degrees.get(node-1),0);
    }



}