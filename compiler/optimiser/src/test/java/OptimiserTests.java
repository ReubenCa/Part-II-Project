import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.carolang.common.interaction_rules.Port;
import com.carolang.common.interaction_rules.Wire;

public class OptimiserTests {
    @Test
    public void WireCollapserCheck()
    {
        Port port1 = new Port();
        Port port2 = new Port();
        Port port3 = new Port();
        Wire wire1 = new Wire(port1, port2);
        Wire wire2 = new Wire(port2, port3);
        Set<Wire> set = Set.of(wire1, wire2);
        Set<Wire> CollapseSet = Wire.flattenWires(set);
        assertTrue(CollapseSet.size() == 1);
    }

    @Test
    public void WireCollapserCheck2()
    {
        Port port1 = new Port();
        Port port2 = new Port();
        Port port3 = new Port();
        Port port4 = new Port();
        Wire wire1 = new Wire(port2, port1);
        Wire wire2 = new Wire(port3, port2);
        Wire wire3 = new Wire(port4, port3);
        Set<Wire> set = Set.of(wire1, wire2, wire3);
        Set<Wire> CollapseSet = Wire.flattenWires(set);
        assertTrue(CollapseSet.size() == 1);
    }

    @Test
    public void WireCollapserCheck3()
    {
        Port port1 = new Port();
        Port port2 = new Port();
        Port port3 = new Port();
        Port port4 = new Port();
        Port port5 = new Port();
        Wire wire1 = new Wire(port2, port1);
        Wire wire2 = new Wire(port3, port2);
        Wire wire3 = new Wire(port3, port4);
        Wire wire4 = new Wire(port5, port4);
        Set<Wire> set = Set.of(wire1, wire2, wire3, wire4);
        Set<Wire> CollapseSet = Wire.flattenWires(set);
        assertTrue(CollapseSet.size() == 1);
    }

    
    public void repeatedlyCheck()
    {
        for(int i = 0; i < 1000000; i++)
        {
            WireCollapserCheck();
            WireCollapserCheck2();
            WireCollapserCheck3();
        }
    }
}
