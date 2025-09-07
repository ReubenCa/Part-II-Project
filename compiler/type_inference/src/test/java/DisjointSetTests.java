import org.junit.jupiter.api.Test;

import com.carolang.DisjointSet;

public class DisjointSetTests {
    @Test
    public void Test1()
    {
        DisjointSet<Integer> set = new DisjointSet<>();
        set.AddItem(1);
        set.AddItem(2);
        set.mergeSets(1, 2);
        assert(set.getSets().size() == 1);
        assert(set.getSets().iterator().next().size() == 2);
    }

    @Test
    public void Test2()
    {
        DisjointSet<Integer> set = new DisjointSet<>();
        set.AddItem(1);
        set.AddItem(2);
        set.AddItem(3);
        set.mergeSets(1, 2);
        assert(set.getSets().size() == 2);
    }

    @Test
    public void Test3()
    {
        DisjointSet<Integer> set = new DisjointSet<>();
        set.AddItem(1);
        set.AddItem(2);
        set.AddItem(3);
        set.AddItem(3);
        set.AddItem(2);
        set.mergeSets(1, 2);
        assert(set.getSets().size() == 2);
    }

    @Test
    public void Test4()
    {
        DisjointSet<Integer> set = new DisjointSet<>();
        set.AddItem(1);
        set.AddItem(2);
        set.AddItem(3);
        set.AddItem(4);
        set.AddItem(5);
        set.AddItem(6);
        set.mergeSets(1, 2);
        set.mergeSets(2, 3);
        set.mergeSets(4,5);
        set.mergeSets(3,5);
        set.mergeSets(2,6);
        assert(set.getSets().size() == 1);
    }

}
