import java.util.ArrayList;
import org.checkerframework.checker.tainting.qual.Untainted;

// @below-java11-jdk-skip-test
public class Issue4170 {
  public void method1() {
    var list = new ArrayList<@Untainted String>();
    var stream = list.stream();
  }

  public void method2() {
    var list = new ArrayList<String>();
    var stream = list.stream();
  }
}
