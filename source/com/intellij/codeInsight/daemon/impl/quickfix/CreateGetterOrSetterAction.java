package com.intellij.codeInsight.daemon.impl.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PropertyUtil;
import com.intellij.util.IncorrectOperationException;

import java.text.MessageFormat;

/**
 * @author ven
 */
public class CreateGetterOrSetterAction implements IntentionAction{
  private final boolean myCreateGetter;
  private final boolean myCreateSetter;
  private final PsiField myField;
  private final String myPropertyName;

  public CreateGetterOrSetterAction(boolean createGetter, boolean createSetter, PsiField field) {
    myCreateGetter = createGetter;
    myCreateSetter = createSetter;
    myField = field;
    Project project = field.getProject();
    myPropertyName = PropertyUtil.suggestPropertyName(project, field);
  }

  public String getText() {
    String what = "";
    if (myCreateGetter) {
      what += "getter";
    }
    if (myCreateSetter) {
      if (myCreateGetter) what += " and ";
      what += "setter";
    }
    return MessageFormat.format("Create {0} for ''{1}''", new Object[]{what, myField.getName()});
  }

  public String getFamilyName() {
    return "Create Accessor for Unused Field";
  }

  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    if (!myField.isValid()) return false;
    PsiClass aClass = myField.getContainingClass();
    if (aClass == null) {
      return false;
    }
    if (myCreateGetter && PropertyUtil.findPropertyGetter(aClass, myPropertyName, myField.hasModifierProperty(PsiModifier.STATIC), false) != null) {
      return false;
    }
    if (myCreateSetter && PropertyUtil.findPropertySetter(aClass, myPropertyName, myField.hasModifierProperty(PsiModifier.STATIC), false) != null) {
      return false;
    }
    return true;
  }

  public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiClass aClass = myField.getContainingClass();
    if (myCreateGetter) {
      aClass.add(PropertyUtil.generateGetterPrototype(myField));
    }
    if (myCreateSetter) {
      aClass.add(PropertyUtil.generateSetterPrototype(myField));
    }
  }

  public boolean startInWriteAction() {
    return true;
  }
}
