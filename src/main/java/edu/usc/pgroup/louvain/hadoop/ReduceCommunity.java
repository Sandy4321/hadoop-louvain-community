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

import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import org.apache.commons.math.optimization.VectorialConvergenceChecker;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import sun.tools.jar.resources.jar;

import java.io.*;
import java.util.*;

/**
 * Created by Charith Wickramaarachchi on 6/30/14.
 */
public class ReduceCommunity extends Reducer<Text, BytesWritable, Text, Text> {

    private double precision = 0.000001;
    private boolean verbose;
    int display_level = -1;
    String outpath;

    @Override
    protected void reduce(Text key, Iterable<BytesWritable> values, Context context) throws IOException, InterruptedException {

        Graph g = null;
        try {
            System.out.println("********************REDUCE****************");
            g = reconstructGraph(values);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InterruptedException(e.toString());
        }
        System.out.println("REDUCE...... GRAPH DONE");
        Community c = new Community(g, -1, precision);

        double mod = c.modularity();

        boolean improvement = true;


        int level = 2;

        if (verbose) {
            System.out.print("level " + level);
            System.out.print("  start computation");
            System.out.println("  network size: "
                    + c.getG().getNb_nodes() + " nodes, "
                    + c.getG().getNb_links() + " links, "
                    + c.getG().getTotal_weight() + " weight.");
        }


        improvement = c.one_level();
        double new_mod = c.modularity();

        if (++level == display_level)
            g.display();
        if (display_level == -1) {
            String filePath = outpath + File.separator + "out_" + (level-1) + "_r.txt";
            try {
                c.display_partition(filePath);
            } catch (Exception e) {
                e.printStackTrace();
                throw  new InterruptedException(e.toString());
            }
        }

        Graph g2 = c.partition2graph_binary();

        if (verbose) {
            System.out.println("  network size: "
                    + c.getG().getNb_nodes() + " nodes, "
                    + c.getG().getNb_links() + " links, "
                    + c.getG().getTotal_weight() + " weight.");
        }


        c = new Community(g2, -1, precision);


        mod = new_mod;

        do {


            if (verbose) {
                System.out.print("level in loop" + level);
                System.out.print("  start computation");
                System.out.println("  network size: "
                        + c.getG().getNb_nodes() + " nodes, "
                        + c.getG().getNb_links() + " links, "
                        + c.getG().getTotal_weight() + " weight.");
            }

            improvement = c.one_level();
            new_mod = c.modularity();

            if (++level == display_level)
                g.display();
            if (display_level == -1) {

                String filePath = outpath + File.separator + "out_" + (level-1) + "_r.txt";
                try {
                    c.display_partition(filePath);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw  new InterruptedException(e.toString());
                }
            }

            g2 = c.partition2graph_binary();

            if (verbose) {
                System.out.print("  network size: "
                        + c.getG().getNb_nodes() + " nodes, "
                        + c.getG().getNb_links() + " links, "
                        + c.getG().getTotal_weight() + " weight.");
            }


            c = new Community(g2, -1, precision);


            mod = new_mod;


        } while (improvement);

    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        Configuration configuration = context.getConfiguration();
        verbose = configuration.getBoolean(LouvainMR.VERBOSE, false);
        precision = configuration.getDouble(LouvainMR.PRECISION, 0.000001);
        display_level = configuration.getInt(LouvainMR.DISPLAY_LEVEL, -1);
        this.outpath = configuration.get(LouvainMR.OUT_PATH);
        System.out.println("verbose = " + verbose);
        System.out.println("display_level = " + display_level);
        System.out.println("outpath = " + outpath);

        super.setup(context);
    }

    private Graph reconstructGraph(Iterable<BytesWritable> values) throws Exception {

        Iterator<BytesWritable> it = values.iterator();

        SortedMap<Integer, GraphMessage> map = new TreeMap<Integer, GraphMessage>();

        //Load data
        while (it.hasNext()) {
            BytesWritable bytesWritable = it.next();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytesWritable.getBytes());

            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                GraphMessage msg = (GraphMessage) objectInputStream.readObject();
                map.put(msg.getCurrentPartition(), msg);
            } catch (IOException e) {
                e.printStackTrace();
                throw new Exception(e);
            }

        }

        // Renumber


        int gap = 0;
        int degreeGap = 0;
        Path pt = new Path(outpath + File.separator +"Map-Partition-Sizes");
        FileSystem fs = FileSystem.get(new Configuration());

        if (fs.exists(pt)) {
            fs.delete(pt, true);

        }


        BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fs.create(pt, true)));

        PrintWriter out = new PrintWriter(br);

        for (int i = 0; i < map.keySet().size(); i++) {

            GraphMessage msg = map.get(i);
            long currentDegreelen = msg.getDegrees()[msg.getDegrees().length -1];
            if (i != 0) {
                for (int j = 0; j < msg.getLinks().length; j++) {
                    msg.getLinks()[j] += gap;
                }

                for (int j = 0; j < msg.getRemoteMap().length; j++) {
                    msg.getRemoteMap()[j].source += gap;
                }

                for (int j = 0; j < msg.getN2c().length; j++) {
                    msg.getN2c()[j] += gap;
                }

                for (int j = 0; j < msg.getDegrees().length; j++) {
                    msg.getDegrees()[j] += degreeGap;




                }

            }


            out.println("" + i + "," + msg.getNb_nodes());
            gap += msg.getNb_nodes();
            degreeGap += currentDegreelen;
        }

        out.flush();
        out.close();
        //Integrate

        Graph graph = new Graph();

        for (int i = 0; i < map.keySet().size(); i++) {
            GraphMessage msg = map.get(i);

            Collections.addAll(graph.getDegrees().getList(), msg.getDegrees());
            Collections.addAll(graph.getLinks().getList(), msg.getLinks());
            Collections.addAll(graph.getWeights().getList(), msg.getWeights());

            graph.setNb_links(graph.getNb_links() + msg.getNb_links());
            graph.setNb_nodes((int) (graph.getNb_nodes() + msg.getNb_nodes()));
            graph.setTotal_weight(graph.getTotal_weight() + msg.getTotal_weight());

        }

        //Merge local done.


        Map<Integer,Vector<Integer>> remoteEdges = new HashMap<Integer, Vector<Integer>>();
        Map<Integer,Vector<Float>> remoteWeighs = new HashMap<Integer, Vector<Float>>();

        for(int i=0; i < map.keySet().size(); i++) {
            Map<HashMap.SimpleEntry<Integer,Integer>, Float> m = new HashMap<AbstractMap.SimpleEntry<Integer, Integer>, Float>();

            GraphMessage msg = map.get(i);
            for (int j = 0; j < msg.getRemoteMap().length; j++) {

                RemoteMap remoteMap = msg.getRemoteMap()[j];

                int sink = remoteMap.sink;
                int sinkPart = remoteMap.sinkPart;

                int target = map.get(sinkPart).getN2c()[sink];

                HashMap.SimpleEntry<Integer,Integer> key = new HashMap.SimpleEntry<Integer,Integer>(remoteMap.source,target);
                if(m.containsKey(key)) {
                    m.put(key,m.get(key) + 1.0f);
                } else {
                    m.put(key,1.0f);
                }
            }

            graph.setNb_links(graph.getNb_links() + m.size());

            Iterator<HashMap.SimpleEntry<Integer,Integer>> itr = m.keySet().iterator();


            while (itr.hasNext()) {

                HashMap.SimpleEntry<Integer,Integer> key = itr.next();
                float w = m.get(key);

                if(remoteEdges.containsKey(key.getKey())) {

                    remoteEdges.get(key.getKey()).getList().add(key.getValue());

                    if (remoteWeighs.containsKey(key.getKey())) {
                        remoteWeighs.get(key.getKey()).getList().add(w);
                    }

                } else {
                    Vector<Integer> list = new Vector<Integer>();
                    list.getList().add(key.getValue());
                    remoteEdges.put(key.getKey(),list);

                    Vector<Float> wList = new Vector<Float>();
                    wList.getList().add(w);
                    remoteWeighs.put(key.getKey(),wList);
                }


            }

        }

        graph.addRemoteEdges(remoteEdges,remoteWeighs);

        //Merge Remote Done


        return graph;

    }
}
