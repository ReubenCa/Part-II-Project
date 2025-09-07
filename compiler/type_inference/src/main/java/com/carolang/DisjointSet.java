package com.carolang;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

//Not particularly optimised don't think there is enough of a reason to
//Although it is a shame to have done a lecture on optimal disjoint sets and not use them
public class DisjointSet<T> {
    class marker {
        private marker groupLeader;
        int size = 1;

        marker() {
            this.groupLeader = this;
        }

        marker getGroupLeader() {
            if (groupLeader == this) {
                return this;
            }
            marker r = groupLeader.getGroupLeader();
            groupLeader = r;
            return r;
        }

        void merge(marker toMerge) {
            marker leader = getGroupLeader();
            leader.size += toMerge.size;
            toMerge.getGroupLeader().groupLeader = leader;

        }

    }

    Map<T, marker> elements = new HashMap<>();

    public void AddItem(T item) {
        if(elements.containsKey(item))
        {
            return;
        }
        assert(item != null);
        
        elements.put(item, new marker());
    }

    public Set<Set<T>> getSets() {
        Map<marker, Set<T>> setMap = new HashMap<>();
        Set<Set<T>> toReturn = new HashSet<Set<T>>();
        for (Entry<T, marker> entry : elements.entrySet()) {
            marker e = entry.getValue();
            marker gl = e.getGroupLeader();
            T data = entry.getKey();
            if (setMap.containsKey(gl)) {
                Set<T> relevantSet = setMap.get(gl);
                relevantSet.add(data);
            } else {
                HashSet<T> newSet = new HashSet<>();
                newSet.add(data);
                setMap.put(gl, newSet);
                toReturn.add(newSet);
            }
        }
        return toReturn;
    }

    public void mergeSets(T item1, T item2) {
        marker m1 = elements.get(item1);
        marker m2 = elements.get(item2);
        assert (m1 != null && m2 != null);
        if (m1.size < m2.size) {
            m2.merge(m1);
        } else {
            m1.merge(m2);
        }
    }

    public Set<T> getSet(T item)
    {
        marker groupLeader = elements.get(item).getGroupLeader();
        Set<T> toReturn = new HashSet<>();
        for(Entry<T, marker> entry : elements.entrySet())
        {
            if(entry.getValue().getGroupLeader().equals(groupLeader))
            {
                toReturn.add(entry.getKey());
            }
        }
        return toReturn;
    }

    public boolean areInSameSet(T item1, T item2)
    {
        return elements.get(item1).getGroupLeader().equals(elements.get(item2).getGroupLeader());
    }

}
