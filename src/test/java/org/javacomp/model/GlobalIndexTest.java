package org.javacomp.model;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public class GlobalIndexTest {
  @Rule public MockitoRule mrule = MockitoJUnit.rule();

  @Mock private Symbol symbol1;
  @Mock private Symbol symbol2;
  @Mock private Symbol symbol3;
  @Mock private Symbol symbol4;

  @Before
  public void setUpMocks() {
    when(symbol1.getSimpleName()).thenReturn("symbol1");
    when(symbol2.getSimpleName()).thenReturn("symbol2");
    when(symbol3.getSimpleName()).thenReturn("symbol3");
    when(symbol4.getSimpleName()).thenReturn("symbol4");
  }

  private GlobalIndex globalIndex = new GlobalIndex();

  @Test
  public void addFilesShouldCreatePackages() {
    FileIndex fileIndex1 = new FileIndex(ImmutableList.of("foo", "bar"));
    FileIndex fileIndex2 = new FileIndex(ImmutableList.of("foo", "bar", "baz"));
    FileIndex fileIndex3 = new FileIndex(ImmutableList.of("foo", "baz"));
    FileIndex fileIndex4 = new FileIndex(ImmutableList.of("fxx"));
    fileIndex1.addSymbol(symbol1);
    fileIndex2.addSymbol(symbol2);
    fileIndex3.addSymbol(symbol3);
    fileIndex4.addSymbol(symbol4);

    globalIndex.addOrReplaceFileIndex("filename1", fileIndex1);
    globalIndex.addOrReplaceFileIndex("filename2", fileIndex2);
    globalIndex.addOrReplaceFileIndex("filename3", fileIndex3);
    globalIndex.addOrReplaceFileIndex("filename4", fileIndex4);

    assertThat(globalIndex.getAllSymbols().keys()).containsExactly("foo", "fxx");

    PackageIndex foo = getPackage(globalIndex, "foo").getChildIndex();
    PackageIndex fooBar = getPackage(foo, "bar").getChildIndex();
    PackageIndex fooBarBaz = getPackage(fooBar, "baz").getChildIndex();
    PackageIndex fooBaz = getPackage(foo, "baz").getChildIndex();
    PackageIndex fxx = getPackage(globalIndex, "fxx").getChildIndex();

    assertThat(foo.getAllSymbols().keys()).containsExactly("bar", "baz");
    assertThat(fooBar.getAllSymbols().keys()).containsExactly("baz", "symbol1");
    assertThat(fooBarBaz.getAllSymbols().keys()).containsExactly("symbol2");
    assertThat(fooBaz.getAllSymbols().keys()).containsExactly("symbol3");
    assertThat(fxx.getAllSymbols().keys()).containsExactly("symbol4");
  }

  @Test
  public void replaceFileWithSamePackageShouldNotRemovePackage() {
    FileIndex fileIndex1 = new FileIndex(ImmutableList.of("foo", "bar"));
    FileIndex fileIndex2 = new FileIndex(ImmutableList.of("foo", "bar"));

    fileIndex1.addSymbol(symbol1);
    fileIndex2.addSymbol(symbol2);

    globalIndex.addOrReplaceFileIndex("foobar", fileIndex1);
    globalIndex.addOrReplaceFileIndex("foobar", fileIndex2);

    PackageIndex foo = getPackage(globalIndex, "foo").getChildIndex();
    PackageIndex fooBar = getPackage(foo, "bar").getChildIndex();
    assertThat(fooBar.getAllSymbols().keys()).containsExactly("symbol2");
  }

  @Test
  public void replaceFileWithDifferentPackageShouldNotRemovePackage() {
    FileIndex fileIndex1 = new FileIndex(ImmutableList.of("foo", "bar", "baz"));
    FileIndex fileIndex2 = new FileIndex(ImmutableList.of("foo", "bar"));
    FileIndex fileIndex3 = new FileIndex(ImmutableList.of("fxx"));

    fileIndex1.addSymbol(symbol1);
    fileIndex2.addSymbol(symbol2);
    fileIndex3.addSymbol(symbol3);

    globalIndex.addOrReplaceFileIndex("foobar", fileIndex1);

    PackageIndex foo = getPackage(globalIndex, "foo").getChildIndex();
    PackageIndex fooBar = getPackage(foo, "bar").getChildIndex();
    PackageIndex fooBarBaz = getPackage(fooBar, "baz").getChildIndex();
    assertThat(fooBarBaz.getAllSymbols().keys()).containsExactly("symbol1");

    // Replace with fileIndex2. baz is removed from foo.bar. symbol2 from fileIndex2 is indexed.
    globalIndex.addOrReplaceFileIndex("foobar", fileIndex2);
    assertThat(fooBar.getAllSymbols().keys()).containsExactly("symbol2");

    // Replace with fileIndex3. foo package is removed. fxx is added with symbol3 from fileIndex3
    globalIndex.addOrReplaceFileIndex("foobar", fileIndex3);
    assertThat(globalIndex.getAllSymbols().keys()).containsExactly("fxx");
    PackageIndex fxx = getPackage(globalIndex, "fxx").getChildIndex();
    assertThat(fxx.getAllSymbols().keys()).containsExactly("symbol3");
  }

  private PackageSymbol getPackage(SymbolIndex index, String simpleName) {
    return getOnlySymbol(index, simpleName, PackageSymbol.class);
  }

  private <T> T getOnlySymbol(SymbolIndex index, String simpleName, Class<T> symbolClass) {
    List<Symbol> symbols = index.getSymbolsWithName(simpleName);
    assertThat(symbols).hasSize(1);
    Symbol symbol = symbols.get(0);
    assertThat(symbol).isInstanceOf(symbolClass);
    @SuppressWarnings("unchecked")
    T typedSymbol = (T) symbol;
    return typedSymbol;
  }
}
