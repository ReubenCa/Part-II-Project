package com.carolang.common.interaction_rules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

public class Wire
{
    public Wire(Port port1, Port port2) {
        assert(port1 != null && port2 != null);
        this.port1 = port1;
        this.port2 = port2;
    }
    private Port port1;
    private Port port2;
    public Port getPort1() {
        return port1;
    }
    public Port getPort2() {
        return port2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Wire wire = (Wire) o;

        return (port1 == wire.getPort1() && port2 == wire.getPort2()) || (port1 == wire.getPort2() && port2 == wire.getPort1());
    }

    @Override
    public int hashCode() {
        int firstCode = port1 != null ? port1.hashCode() : 0;
        int secondCode = port2 != null ? port2.hashCode() : 0;
        return firstCode ^ secondCode;
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]", port1.toString(), port2.toString());
    }

    public static Set<Wire> flattenWires(Set<Wire> wires) {
        int lastIterationWiresSize = 0;
        do {
            lastIterationWiresSize = wires.size();
            Set<Wire> wiresToRemove = new HashSet<>();
            Set<Wire> wiresToAdd = new HashSet<>();
            for (Wire w : wires) {
                List<Wire> matchingWires = wires.stream().filter(wire2 -> wire2 != w).filter(
                        wire2 -> Sets.intersection(Set.of(w.getPort1(), w.getPort2()),
                                Set.of(wire2.getPort1(), wire2.getPort2())).size() >= 1).toList();
                boolean thereWasAMatch = matchingWires.size() > 0;
                for (Wire matchingWire : matchingWires) {
                    Wire mergedWire;
                    if (w.getPort1() == matchingWire.getPort1()) {
                        mergedWire = new Wire(w.getPort2(), matchingWire.getPort2());
                    } else if (w.getPort1() == matchingWire.getPort2()) {
                        mergedWire = new Wire(w.getPort2(), matchingWire.getPort1());
                    } else if (w.getPort2() == matchingWire.getPort1()) {
                        mergedWire = new Wire(w.getPort1(), matchingWire.getPort2());
                    } else {
                        mergedWire = new Wire(w.getPort1(), matchingWire.getPort1());
                    }
                    wiresToRemove.add(w);
                    wiresToRemove.add(matchingWire);
                    wiresToAdd.add(mergedWire);
                    break;
                }
                if(thereWasAMatch)
                {
                    break;
                }
            
            }
            wires = wires.stream().filter(w -> !wiresToRemove.contains(w)).collect(Collectors.toSet());
            wires.addAll(wiresToAdd);

        } while (lastIterationWiresSize > wires.size());
        return wires;
    }
}