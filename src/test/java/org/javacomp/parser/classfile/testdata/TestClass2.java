package test.data;

public interface TestClass2 {
  // This generates an anonymous class and adds an InnerClass attribute for this
  // anonymous class with both outer_class_info_index and inner_class_name_index
  // being 0.
  TestClass2 INSTANCE = new TestClass2() {};
}
