import java.util.*;

/**
 * Created by huxq on 16/1/24.
 */

class  Node{
    public String nodeName;
    public Hashtable<String, Double> PDistribution;
    public Set<String> UniqueVals;
    public List<String> OriginalVals;
    public double totalMeasureMent;

    //colvals and measurement are two columns of the original table
    public Node(String nodeName, Double totalMeasureMent, List<String> colvals, List<Double> measurement) {
        this.nodeName = nodeName;
        this.totalMeasureMent = totalMeasureMent;
        this.OriginalVals = colvals;
        this.PDistribution = new Hashtable<String, Double>();

        int tablelength = colvals.size();
        String dimen;
        for(int i=0; i<tablelength ;++i){
            dimen = colvals.get(i);
            if(PDistribution.containsKey(dimen))
                PDistribution.put(dimen, PDistribution.get(dimen) + measurement.get(i));
            else
                PDistribution.put(dimen, measurement.get(i));
        }
        this.UniqueVals= PDistribution.keySet();
    }
}

class Edge{
    public List<Node> Nodes;
    public Double CMI;

    public Edge(Node node1, Node node2, double CMI){
        Nodes = new ArrayList<Node>();
        Nodes.add(node1);
        Nodes.add(node2);
        this.CMI = CMI;
    }
}

class Graph{

}

class BayesianNetwork{
    public Hashtable<String, Node> ValsColumnStore;
    public List<Double> Measurements;
    public Double TotalMeasurement;

    public List<Node> Nodes;
    public List<List<Node>> E;
    public List<Edge> AllEdges;

    public BayesianNetwork(List<String> ColumnNames, List<List<String>> ColumnVals,List<Double> Measurements){
        this.Measurements = Measurements;
        this.TotalMeasurement = 0.0;
        for(Double mm:Measurements)
            this.TotalMeasurement += mm;

        for(int i=0; i<ColumnNames.size(); ++i){
            Node tmp = new Node(ColumnNames.get(i), this.TotalMeasurement, ColumnVals.get(i), Measurements);
            ValsColumnStore.put(ColumnNames.get(i), tmp);
        }
    }

    public void Draft(){
        int nodesize = Nodes.size();
        //calculate all mutual information
        for(int i=0; i<nodesize-1; ++i)
            for(int j=i+1; j<nodesize; ++j){
                Node node1 = Nodes.get(i);
                Node node2 = Nodes.get(j);
                Double CMI = ConditionalMutualInformation(node1, node2, new ArrayList<Node>());
                this.AllEdges.add(new Edge(node1, node2, CMI));
            }

        //sort edges
        this.AllEdges.sort(new SortByCMI());

        //build draft network
    }

    double ConditionalMutualInformation(Node Xi, Node Xj, List<Node> Conditions){
        double ret = 0;

        //condition#xi exp. chn,male#suv
        Hashtable<String, Double> XiVals = new Hashtable<String, Double>();
        //chn,male#big
        Hashtable<String, Double> XjVals = new Hashtable<String, Double>();
        //chn,male#suv,big
        Hashtable<String, Double> XiXjVals = new Hashtable<String, Double>();
        HashSet<String> ConditionCombinations = new HashSet<String>();
        ConditionCombinations.add("");

        int maxlengh = 10;
        int conditionSize = Conditions.size();
        StringBuffer thekey = new StringBuffer(500);
        for(int i=0; i<maxlengh; ++i){
            Double thislineMeasurement = this.Measurements.get(i);

            for(int j=0; j<conditionSize; ++j){
                thekey.append(Conditions.get(j).OriginalVals.get(i));
                thekey.append(",");
            }
            thekey.deleteCharAt(thekey.length() - 1);
            ConditionCombinations.add(thekey.toString());
            thekey.append("#");

            String xi = Xi.OriginalVals.get(i);
            String xj = Xj.OriginalVals.get(i);

            String xiKey = thekey.toString() + "#" + xi;
            if(!XiVals.containsKey(xiKey))
                XiVals.put(xiKey, thislineMeasurement);
            XiVals.put(xiKey, XiVals.get(xiKey) + thislineMeasurement);

            String xixjKey = thekey.toString() + "#" + xi + "," + xj;
            if(!XiXjVals.containsKey(xixjKey))
                XiXjVals.put(xixjKey, thislineMeasurement);
            XiXjVals.put(xixjKey, XiXjVals.get(xixjKey) + thislineMeasurement);

            String xjKey = thekey.toString() + "#" + xj;
            if(!XjVals.containsKey(xjKey))
                XjVals.put(xjKey, thislineMeasurement);
            XjVals.put(xjKey, XjVals.get(xjKey) + thislineMeasurement);

            thekey.setLength(0);
        }

        //change number to possibility
        Iterator iter0 = XiVals.entrySet().iterator();
        while(iter0.hasNext()){
            Map.Entry entry = (Map.Entry)iter0.next();
            entry.setValue((Double)entry.getValue() / TotalMeasurement);
        }
        Iterator iter1 = XiVals.entrySet().iterator();
        while(iter1.hasNext()) {
            Map.Entry entry = (Map.Entry) iter1.next();
            entry.setValue((Double) entry.getValue() / TotalMeasurement);
        }
        Iterator iter2 = XiVals.entrySet().iterator();
        while(iter2.hasNext()) {
            Map.Entry entry = (Map.Entry) iter2.next();
            entry.setValue((Double) entry.getValue() / TotalMeasurement);
        }

        //calculate mutual information
        for(String condition:ConditionCombinations){
            for(String xi: Xi.UniqueVals) {
                String xiKey = condition + "#" + xi;
                if(!XiVals.containsKey(xiKey))
                    continue;

                for(String xj: Xj.UniqueVals){
                    String xjKey = condition + "#" + xj;
                    if(!XiVals.containsKey(xjKey))
                        continue;

                    String xixjKey = condition + "#" + xi + "," + xj;
                    if(!XiXjVals.contains(xixjKey))
                        continue;

                    Double Pcxi = XiVals.get(xiKey);
                    Double Pcxj = XjVals.get(xjKey);
                    Double Pcxixj = XiXjVals.get(xixjKey);
                    ret += Pcxixj * Math.log(Pcxixj/(Pcxi * Pcxj));
                }
            }
        }

        return ret;
    }
}

class SortByCMI implements Comparator {
    public int compare(Object o1, Object o2) {
        Edge e1 = (Edge) o1;
        Edge e2= (Edge) o2;
        if (e1.CMI > e2.CMI)
            return 1;
        return 0;
    }
}
