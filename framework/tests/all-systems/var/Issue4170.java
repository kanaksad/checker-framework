import java.util.ArrayList;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue4170 {
  public void method1() {
    var v = new ArrayList<@Untainted String>();
    v.add("str1");
    System.out.println(v.get(0));
  }
}
