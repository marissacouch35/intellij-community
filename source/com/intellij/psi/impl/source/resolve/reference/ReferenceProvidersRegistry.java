package com.intellij.psi.impl.source.resolve.reference;

import com.intellij.ant.impl.dom.impl.RegisterInPsi;
import com.intellij.lang.properties.PropertiesReferenceProvider;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiPlainTextFile;
import com.intellij.psi.filters.*;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.psi.filters.position.TokenTypeFilter;
import com.intellij.psi.impl.meta.MetaRegistry;
import com.intellij.psi.impl.source.jsp.jspJava.JspDirective;
import com.intellij.psi.impl.source.resolve.ResolveUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.manipulators.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.*;
import com.intellij.psi.xml.*;
import com.intellij.xml.util.HtmlReferenceProvider;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 27.03.2003
 * Time: 17:13:45
 * To change this template use Options | File Templates.
 */
public class ReferenceProvidersRegistry implements ProjectComponent {
  private final List<Class> myTempScopes = new ArrayList<Class>();
  private final List<ProviderBinding> myBindingsWithoutClass = new ArrayList<ProviderBinding>();
  private final Map<Class,ProviderBinding> myBindingsMap = new HashMap<Class,ProviderBinding>();
  private final List<Pair<Class, ElementManipulator>> myManipulators = new ArrayList<Pair<Class, ElementManipulator>>();
  private final Map<ReferenceProviderType,PsiReferenceProvider> myReferenceTypeToProviderMap = new HashMap<ReferenceProviderType, PsiReferenceProvider>(5);


  static public class ReferenceProviderType {
    private String myId;
    public ReferenceProviderType(@NonNls String id) { myId = id; }
    public String toString() { return myId; }
  }

  private static ReferenceProviderType PROPERTIES_FILE_KEY_PROVIDER = new ReferenceProviderType("Properties File Key Provider");
  private static ReferenceProviderType CLASS_REFERENCE_PROVIDER = new ReferenceProviderType("Class Reference Provider");
  private static ReferenceProviderType PATH_REFERENCES_PROVIDER = new ReferenceProviderType("Path References Provider");
  public static ReferenceProviderType DYNAMIC_PATH_REFERENCES_PROVIDER = new ReferenceProviderType("Dynamic Path References Provider");
  public static ReferenceProviderType CSS_CLASS_OR_ID_KEY_PROVIDER = new ReferenceProviderType("Css Class or ID Provider");
  public static ReferenceProviderType URI_PROVIDER = new ReferenceProviderType("Uri references provider");

  public static final ReferenceProvidersRegistry getInstance(Project project) {
    return project.getComponent(ReferenceProvidersRegistry.class);
  }

  public void registerTypeWithProvider(ReferenceProviderType type, PsiReferenceProvider provider) {
    myReferenceTypeToProviderMap.put(type, provider);
  }

  private ReferenceProvidersRegistry() {
    // Temp scopes declarations
    myTempScopes.add(PsiIdentifier.class);

    // Manipulators mapping
    registerManipulator(XmlAttributeValue.class, new XmlAttributeValueManipulator());
    registerManipulator(PsiPlainTextFile.class, new PlainFileManipulator());
    registerManipulator(XmlToken.class, new XmlTokenManipulator());
    registerManipulator(PsiLiteralExpression.class, new StringLiteralManipulator());
    registerManipulator(XmlTag.class, new XmlTagValueManipulator());
    // Binding declarations

    myReferenceTypeToProviderMap.put(CLASS_REFERENCE_PROVIDER, new JavaClassReferenceProvider());
    myReferenceTypeToProviderMap.put(PATH_REFERENCES_PROVIDER, new JspxIncludePathReferenceProvider());
    myReferenceTypeToProviderMap.put(DYNAMIC_PATH_REFERENCES_PROVIDER, new JspxDynamicPathReferenceProvider());
    myReferenceTypeToProviderMap.put(PROPERTIES_FILE_KEY_PROVIDER, new PropertiesReferenceProvider());

    registerXmlAttributeValueReferenceProvider(
      new String[]{"class", "type"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new TextFilter("useBean"),
            new NamespaceFilter(XmlUtil.JSP_URI)
          ), 2
        )
      ), getProviderByType(CLASS_REFERENCE_PROVIDER)
    );

    RegisterInPsi.referenceProviders(this);

    registerXmlAttributeValueReferenceProvider(
      new String[]{"extends"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new OrFilter(
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("directive.page")
              ),
              new AndFilter(
                new ClassFilter(JspDirective.class),
                new TextFilter("page")
              )
            ),
            new NamespaceFilter(XmlUtil.JSP_URI)
          ),
          2
        )
      ), getProviderByType(CLASS_REFERENCE_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"type"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new OrFilter(
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("directive.attribute")
              ),
              new AndFilter(
                new ClassFilter(JspDirective.class),
                new TextFilter("attribute")
              )
            ),
            new NamespaceFilter(XmlUtil.JSP_URI)
          ),
          2
        )
      ), getProviderByType(CLASS_REFERENCE_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"variable-class"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new OrFilter(
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("directive.variable")
              ),
              new AndFilter(
                new ClassFilter(JspDirective.class),
                new TextFilter("variable")
              )
            ),
            new NamespaceFilter(XmlUtil.JSP_URI)
          ),
          2
        )
      ), getProviderByType(CLASS_REFERENCE_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[] { "import" },
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new OrFilter(
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("directive.page")
              ),
              new AndFilter(
                new ClassFilter(JspDirective.class),
                new TextFilter("page")
              )
            ),
            new NamespaceFilter(XmlUtil.JSP_URI)
          ),
          2
        )
      ),
      new JspImportListReferenceProvider()
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"errorPage"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new NamespaceFilter(XmlUtil.JSP_URI),
            new OrFilter(
              new AndFilter(
                new ClassFilter(JspDirective.class),
                new TextFilter("page")
              ),
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("directive.page")
              ))
          ), 2
        )
      ), getProviderByType(PATH_REFERENCES_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"file"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new NamespaceFilter(XmlUtil.JSP_URI),
            new OrFilter(
              new AndFilter(
                new ClassFilter(JspDirective.class),
                new TextFilter("include")
              ),
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("directive.include")
              ))
          ), 2
        )
      ), getProviderByType(PATH_REFERENCES_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"value"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new NamespaceFilter(XmlUtil.JSTL_CORE_URIS),
            new AndFilter(
              new ClassFilter(XmlTag.class),
              new TextFilter("url")
            )
          ), 2
        )
      ), getProviderByType(DYNAMIC_PATH_REFERENCES_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"url"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new NamespaceFilter(XmlUtil.JSTL_CORE_URIS),
            new AndFilter(
              new ClassFilter(XmlTag.class),
              new OrFilter(
                new TextFilter("import"),
                new TextFilter("redirect")
              )
            )
          ), 2
        )
      ), getProviderByType(DYNAMIC_PATH_REFERENCES_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"key"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new OrFilter(
              new NamespaceFilter(XmlUtil.JSTL_FORMAT_URIS),
              new NamespaceFilter(XmlUtil.STRUTS_BEAN_URI)
            ),
            new AndFilter(
              new ClassFilter(XmlTag.class),
              new TextFilter("message")
            )
          ), 2
        )
      ), getProviderByType(PROPERTIES_FILE_KEY_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"altKey","titleKey","pageKey","srcKey"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new NamespaceFilter(XmlUtil.STRUTS_HTML_URI),
            new ClassFilter(XmlTag.class)
          ), 2
        )
      ), getProviderByType(PROPERTIES_FILE_KEY_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"code"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new NamespaceFilter(XmlUtil.SPRING_URI),
            new AndFilter(
              new ClassFilter(XmlTag.class),
              new TextFilter("message", "theme")
            )
          ), 2
        )
      ), getProviderByType(PROPERTIES_FILE_KEY_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"page"},
      new ScopeFilter(
        new ParentElementFilter(
          new OrFilter(
            new AndFilter(
              new NamespaceFilter(XmlUtil.JSP_URI),
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("include","forward")
              )
            ),
            new AndFilter(
              new NamespaceFilter(XmlUtil.STRUTS_HTML_URI),
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("rewrite")
              )
            )
          ), 2
        )
      ), getProviderByType(DYNAMIC_PATH_REFERENCES_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[]{"tagdir"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new NamespaceFilter(XmlUtil.JSP_URI),
            new AndFilter(
              new ClassFilter(JspDirective.class),
              new TextFilter("taglib")
            )
          ), 2
        )
      ), getProviderByType(PATH_REFERENCES_PROVIDER)
    );

    registerXmlAttributeValueReferenceProvider(
      new String[] { "uri" },
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new NamespaceFilter(XmlUtil.JSP_URI),
            new AndFilter(
              new ClassFilter(JspDirective.class),
              new TextFilter("taglib")
            )
          ), 2
        )
      ),
      new JspUriReferenceProvider()
    );

    final JavaClassListReferenceProvider classListProvider = new JavaClassListReferenceProvider();
    registerXmlAttributeValueReferenceProvider(
      null,
      new NotFilter(new ParentElementFilter(new NamespaceFilter(XmlUtil.ANT_URI), 2)),
      classListProvider
    );

    registerReferenceProvider(new TokenTypeFilter(XmlTokenType.XML_DATA_CHARACTERS), XmlToken.class,
                              classListProvider);

    registerXmlTagReferenceProvider(
      new String[] {
        "function-class", "tag-class", "tei-class", "variable-class", "type", "path",
        "function-signature", "name", "name-given"
      },
      null,
      true,
      new TaglibReferenceProvider( getProviderByType(CLASS_REFERENCE_PROVIDER) )
    );
    
    registerXmlTagReferenceProvider(
      new String[] {
              "render-kit-class","render-class","managed-bean-class","attribute-class","component-class", "converter-for-class", 
              "converter-class", "property-class", "key-class", "value-class", "reference-bean-class", "validator-class"
            },
      null,
      true,
      new TaglibReferenceProvider( getProviderByType(CLASS_REFERENCE_PROVIDER) )
    );
    
    registerXmlTagReferenceProvider(
      new String[] {
        "property-name"
      },
      null,
      true,
      new JSFReferencesProvider()
    );

    final DtdReferencesProvider dtdReferencesProvider = new DtdReferencesProvider();
    //registerReferenceProvider(null, XmlEntityDecl.class,dtdReferencesProvider);
    registerReferenceProvider(null, XmlEntityRef.class,dtdReferencesProvider);
    registerReferenceProvider(null, XmlDoctype.class,dtdReferencesProvider);
    registerReferenceProvider(null, XmlElementDecl.class,dtdReferencesProvider);
    registerReferenceProvider(null, XmlAttlistDecl.class,dtdReferencesProvider);
    registerReferenceProvider(null, XmlElementContentSpec.class,dtdReferencesProvider);

    URIReferenceProvider uriProvider = new URIReferenceProvider();

    registerTypeWithProvider(URI_PROVIDER,uriProvider);
    registerXmlAttributeValueReferenceProvider(
      null,
      dtdReferencesProvider.getSystemReferenceFilter(),
      uriProvider
    );

    //registerReferenceProvider(PsiPlainTextFile.class, new JavaClassListReferenceProvider());

    HtmlReferenceProvider provider = new HtmlReferenceProvider();
    registerXmlAttributeValueReferenceProvider(
      new String[] {
        "src", "href", "action", "background", "width", "height", "type", "bgcolor", "color", "vlink", "link", "alink", "text"
      },
      provider.getFilter(),
      false,
      provider
    );

    final PsiReferenceProvider filePathReferenceProvider = new FilePathReferenceProvider();
    registerReferenceProvider(PsiLiteralExpression.class, filePathReferenceProvider);

    final SchemaReferencesProvider schemaReferencesProvider = new SchemaReferencesProvider();
    registerXmlAttributeValueReferenceProvider(
      new String[] {"ref","type","base","name","substitutionGroup","memberTypes"},
      new ScopeFilter(
        new ParentElementFilter(
          new NamespaceFilter(MetaRegistry.SCHEMA_URIS), 2
        )
      ),
      schemaReferencesProvider
    );

    registerXmlAttributeValueReferenceProvider(
      new String[] {"xsi:type"},
      null,
      schemaReferencesProvider
    );

    registerXmlAttributeValueReferenceProvider(
      new String[] {"xsi:noNamespaceSchemaLocation"},
      null,
      uriProvider
    );

    registerXmlAttributeValueReferenceProvider(
      new String[] {"schemaLocation"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new NamespaceFilter(MetaRegistry.SCHEMA_URIS),
            new AndFilter(
              new ClassFilter(XmlTag.class),
              new TextFilter("import","include")
            )
          ), 2
        )
      ),
      uriProvider
    );

    registerXmlAttributeValueReferenceProvider(
      null,
      uriProvider.getNamespaceAttributeFilter(),
      uriProvider
    );

    final JspReferencesProvider jspReferencesProvider = new JspReferencesProvider();

    registerXmlAttributeValueReferenceProvider(
      new String[] {"fragment","name","property","id","name-given","dynamic-attributes"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new ClassFilter(XmlTag.class),
            new NamespaceFilter(
              new String[] {
                XmlUtil.JSP_URI,
                XmlUtil.STRUTS_BEAN_URI,
                XmlUtil.STRUTS_LOGIC_URI
              }
            )
          ), 2
        )
      ),
      jspReferencesProvider
    );

    registerXmlAttributeValueReferenceProvider(
      new String[] {"var"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new ClassFilter(XmlTag.class),
            new NamespaceFilter(XmlUtil.JSTL_CORE_URIS)
          ), 2
        )
      ),
      jspReferencesProvider
    );

    registerXmlAttributeValueReferenceProvider(
      new String[] {"scope"},
      null,
      jspReferencesProvider
    );
    
    registerXmlAttributeValueReferenceProvider(
      new String[] {"name"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new ClassFilter(XmlTag.class),
            new AndFilter(
              new TextFilter("property"),
              new NamespaceFilter(XmlUtil.SPRING_CORE_URI)
            )
          ), 2
        )
      ),
      new SpringReferencesProvider()
    );
    
    registerXmlAttributeValueReferenceProvider(
      new String[] {"name"},
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new ClassFilter(XmlTag.class),
            new AndFilter(
              new NamespaceFilter(XmlUtil.HIBERNATE_URIS),
              new TextFilter(new String[] { "property","list","map","set", "array", "bag", "idbag", "primitive-array", "many-to-one", "one-to-one"} )
            )
          ), 2
        )
      ),
      new HibernateReferencesProvider()
    );
  }

  public void registerReferenceProvider(ElementFilter elementFilter, Class scope, PsiReferenceProvider provider) {
    if (scope == XmlAttributeValue.class) {
      registerXmlAttributeValueReferenceProvider(null, elementFilter, provider);
      return;
    } else if (scope == XmlTag.class) {
      registerXmlTagReferenceProvider(null, elementFilter, false, provider);
      return;
    }

    if (scope != null) {
      final ProviderBinding providerBinding = myBindingsMap.get(scope);
      if (providerBinding != null) {
        ((SimpleProviderBinding)providerBinding).registerProvider(provider, elementFilter);
        return;
      }
    }
    
    final SimpleProviderBinding binding = new SimpleProviderBinding(scope);
    binding.registerProvider(provider,elementFilter);
    if (scope == null) myBindingsWithoutClass.add(binding);
    else {
      myBindingsMap.put(scope,binding);
    }
  }

  public void registerXmlTagReferenceProvider(@NonNls String[] names, ElementFilter elementFilter, boolean caseSensitive,PsiReferenceProvider provider) {
    XmlTagProviderBinding tagProviderBinding = (XmlTagProviderBinding)myBindingsMap.get(XmlTag.class);
    
    if (tagProviderBinding == null) {
      tagProviderBinding = new XmlTagProviderBinding();
      myBindingsMap.put(XmlTag.class, tagProviderBinding);
    }

    tagProviderBinding.registerProvider(
      names,
      elementFilter,
      caseSensitive, 
      provider
    );
  }
  
  public void registerXmlAttributeValueReferenceProvider(@NonNls String[] attributeNames, ElementFilter elementFilter, boolean caseSensitive,PsiReferenceProvider provider) {
    XmlAttributeValueProviderBinding attributeValueProviderBinding = (XmlAttributeValueProviderBinding)myBindingsMap.get(XmlAttributeValue.class);
    
    if (attributeValueProviderBinding == null) {
      attributeValueProviderBinding = new XmlAttributeValueProviderBinding();
      myBindingsMap.put(XmlAttributeValue.class, attributeValueProviderBinding);
    }

    attributeValueProviderBinding.registerProvider(
      attributeNames,
      elementFilter,
      caseSensitive, 
      provider
    );
  }
  
  public void registerXmlAttributeValueReferenceProvider(@NonNls String[] attributeNames, ElementFilter elementFilter, PsiReferenceProvider provider) {
    registerXmlAttributeValueReferenceProvider(attributeNames, elementFilter, true, provider);
  }

  public PsiReferenceProvider getProviderByType(ReferenceProviderType type) {
    return myReferenceTypeToProviderMap.get(type);
  }

  public void registerReferenceProvider(Class scope, PsiReferenceProvider provider) {
    registerReferenceProvider(null, scope, provider);
  }

  public PsiReferenceProvider[] getProvidersByElement(PsiElement element, Class hintClass) {
    assert hintClass == null || hintClass.isInstance(element);
    
    List<PsiReferenceProvider> ret = new ArrayList<PsiReferenceProvider>(1);
    PsiElement current;
    do {
      current = element;

      if (hintClass != null) {
        final ProviderBinding providerBinding = myBindingsMap.get(hintClass);
        if (providerBinding != null) providerBinding.addAcceptableReferenceProviders(current, ret);
      } else {
        for(ProviderBinding providerBinding:myBindingsMap.values()) {
          providerBinding.addAcceptableReferenceProviders(current, ret);
        }
      }
      
      for (final ProviderBinding binding : myBindingsWithoutClass) {
        binding.addAcceptableReferenceProviders(current, ret);
      }
      element = ResolveUtil.getContext(element);
    }
    while (!isScopeFinal(current.getClass()));

    return ret.size() > 0 ? ret.toArray(new PsiReferenceProvider[ret.size()]) : PsiReferenceProvider.EMPTY_ARRAY;
  }

  public PsiReferenceProvider[] getProvidersByElement(PsiElement element) {
    return getProvidersByElement(element, null);
  }

  public <T extends PsiElement> ElementManipulator<T> getManipulator(T element) {
    if(element == null) return null;

    for (final Pair<Class, ElementManipulator> pair : myManipulators) {
      if (pair.getFirst().isAssignableFrom(element.getClass())) {
        return (ElementManipulator<T>)pair.getSecond();
      }
    }

    return null;
  }

  public <T extends PsiElement> void registerManipulator(Class<T> elementClass, ElementManipulator<T> manipulator) {
    myManipulators.add(new Pair<Class, ElementManipulator>(elementClass, manipulator));
  }

  private boolean isScopeFinal(Class scopeClass) {

    for (final Class aClass : myTempScopes) {
      if (aClass.isAssignableFrom(scopeClass)) {
        return false;
      }
    }
    return true;
  }

  public void projectOpened() {}

  public void projectClosed() {}

  public String getComponentName() {
    return "Reference providers registry";
  }

  public void initComponent() {}

  public void disposeComponent() {}
}
