package com.intellij.psi.impl.source.resolve.reference;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.filters.ElementFilter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 01.04.2003
 * Time: 16:52:28
 * To change this template use Options | File Templates.
 */
public class SimpleProviderBinding implements ProviderBinding {
  private final Class myScope;
  private List<Pair<PsiReferenceProvider,ElementFilter>> myProviderPairs =
    new ArrayList<Pair<PsiReferenceProvider,ElementFilter>>(1);

  public SimpleProviderBinding(Class scope){
    myScope = scope;
  }

  private boolean isAcceptable(PsiElement position, ElementFilter filter){
    if(position == null) return false;

    if (myScope.isAssignableFrom(position.getClass())) {
      return filter == null || filter.isAcceptable(position, position);
    }

    return false;
  }

  public void registerProvider(PsiReferenceProvider provider,ElementFilter elementFilter){
    myProviderPairs.add(new Pair<PsiReferenceProvider, ElementFilter>(provider, elementFilter));
  }

  public void addAcceptableReferenceProviders(@NotNull PsiElement position, @NotNull List<PsiReferenceProvider> list) {
    for(Pair<PsiReferenceProvider,ElementFilter> pair:myProviderPairs) {
      if (isAcceptable(position,pair.second)) {
        list.add(pair.first);
      }
    }
  }
}
