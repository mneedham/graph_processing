package com.maxdemarzi.processing.labelpropagation;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.transaction.state.NeoStoreProvider;
import org.neo4j.tooling.GlobalGraphOperations;

public class LabelPropagationMapStorage implements LabelPropagation {
    private final GraphDatabaseService db;
    private final int nodes;
    private Long2DoubleOpenHashMap labelMap;

    public LabelPropagationMapStorage(GraphDatabaseService db) {
        this.db = db;
        NeoStoreProvider neoStoreProvider = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(NeoStoreProvider.class);
        this.nodes = (int) neoStoreProvider.evaluate().getNodeStore().getHighId();
    }

    @Override
    public void compute(String label, String type, int iterations) {
        RelationshipType relationshipType = DynamicRelationshipType.withName(type);
        labelMap = new Long2DoubleOpenHashMap();
        boolean done = false;
        int iteration = 0;
        try ( Transaction tx = db.beginTx()) {
            ResourceIterator<Node> nodes = db.findNodes(DynamicLabel.label(label));
            while (nodes.hasNext()) {
                Node node = nodes.next();
                labelMap.put(node.getId(), node.getId());
            }

            while (!done) {
                done = true;
                iteration++;

                for( Relationship relationship : GlobalGraphOperations.at(db).getAllRelationships()) {
                    if (relationship.isType(relationshipType)) {
                        long x = relationship.getStartNode().getId();
                        long y = relationship.getEndNode().getId();
                        if (x == y) { continue; }
                        if (labelMap.get(x) > labelMap.get(y)){
                            labelMap.put(x, labelMap.get(y));
                            done = false;
                        } else if (labelMap.get(x) < labelMap.get(y)) {
                            labelMap.put(y, labelMap.get(x));
                            done = false;
                        }
                    }
                }

                if (iteration > iterations) {
                    done = true;
                }
            }
        }
    }

    @Override
    public double getResult(long node) {
        return labelMap != null ? labelMap.getOrDefault(node, -1D) : -1;
    }

    @Override
    public long numberOfNodes() {
        return nodes;
    };

}
