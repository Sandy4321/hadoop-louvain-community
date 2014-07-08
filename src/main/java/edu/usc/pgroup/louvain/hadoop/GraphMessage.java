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

/**
 * Created by Charith Wickramaarachchi on 7/7/14.
 */

import java.io.Serializable;

/**
 * Class <>GraphMessage</> take care of transfering intermediate graph to remote nodes
 */
public class GraphMessage implements Serializable{

    private long degrees[];
    private int links[];
    private float weights[];

    private long nb_links;
    private long nb_nodes;
    private double total_weight;

    private int currentPartition;

    private int[] n2c;

    private Graph.RemoteMap remoteMap[];

    public long[] getDegrees() {
        return degrees;
    }

    public void setDegrees(long[] degrees) {
        this.degrees = degrees;
    }

    public int[] getLinks() {
        return links;
    }

    public void setLinks(int[] links) {
        this.links = links;
    }

    public float[] getWeights() {
        return weights;
    }

    public void setWeights(float[] weights) {
        this.weights = weights;
    }

    public long getNb_links() {
        return nb_links;
    }

    public void setNb_links(long nb_links) {
        this.nb_links = nb_links;
    }

    public long getNb_nodes() {
        return nb_nodes;
    }

    public void setNb_nodes(long nb_nodes) {
        this.nb_nodes = nb_nodes;
    }

    public double getTotal_weight() {
        return total_weight;
    }

    public void setTotal_weight(double total_weight) {
        this.total_weight = total_weight;
    }

    public int[] getN2c() {
        return n2c;
    }

    public void setN2c(int[] n2c) {
        this.n2c = n2c;
    }

    public Graph.RemoteMap[] getRemoteMap() {
        return remoteMap;
    }

    public void setRemoteMap(Graph.RemoteMap[] remoteMap) {
        this.remoteMap = remoteMap;
    }
}
