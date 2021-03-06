package com.weirddev.testme.intellij.resolvers.groovy;

import com.intellij.lang.Language;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.weirddev.testme.intellij.resolvers.to.ResolvedMethodCall;
import com.weirddev.testme.intellij.resolvers.to.ResolvedReference;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrParenthesizedExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.arguments.GrArgumentLabelImpl;

import java.util.*;

/**
 * Date: 09/05/2017
 *
 * @author Yaron Yamin
 */
public class GroovyPsiTreeUtils {

    public static final String GROOVY_LANGUAGE_ID = "Groovy";

    public static boolean isGroovy(Language language) {
        return language == com.intellij.lang.Language.findLanguageByID(GROOVY_LANGUAGE_ID) && LanguageUtils.isPluginEnabled(LanguageUtils.GROOVY_PLUGIN_ID);
    }

    public static PsiType resolveType(PsiElement prevSibling) {
        return prevSibling instanceof GrReferenceExpression ?((GrReferenceExpression) prevSibling).getType():null;
    }

    public static List<ResolvedReference> findReferences(PsiElement psiMethod){

        List<ResolvedReference> resolvedReferences =new ArrayList<ResolvedReference>();
        final Collection<GrReferenceExpression> grReferenceExpressions = PsiTreeUtil.findChildrenOfType(psiMethod, GrReferenceExpression.class);
        for (GrReferenceExpression grReferenceExpression : grReferenceExpressions) {
            final PsiType refType = grReferenceExpression.getType();
            final PsiElement psiElement = grReferenceExpression.resolve();
            if (refType != null && !(psiElement instanceof GrMethod) && (psiElement==null || isGroovy(psiElement.getLanguage()) || !(psiElement instanceof PsiMethod))) {
                final PsiType psiOwnerType = grReferenceExpression.getLastChild()==null?null:resolveOwnerType(grReferenceExpression.getLastChild());
                if (psiOwnerType != null) {
                    resolvedReferences.add(new ResolvedReference(grReferenceExpression.getReferenceName() , refType, psiOwnerType));
                }
            }
        }
        return resolvedReferences;
    }

    public static Set<ResolvedMethodCall> findMethodCalls(PsiElement psiMethod){
        Set<ResolvedMethodCall> methodCalls=new HashSet<ResolvedMethodCall>();
        final Collection<GrCall> grMethodCallExpressions = PsiTreeUtil.findChildrenOfType(psiMethod, GrCall.class);
        for (GrCall grMethodCallExpression : grMethodCallExpressions) {
            final GrArgumentList argumentList = grMethodCallExpression.getArgumentList();
            final ArrayList<String> methodCallArguments = new ArrayList<String>();
            if (argumentList != null) {
                for (PsiElement psiElement : argumentList.getChildren()) {
                    if (psiElement instanceof PsiJavaToken) {
                        continue;
                    }
                    methodCallArguments.add(psiElement.getText()==null?"":psiElement.getText().trim());
                }
            }
            final PsiMethod psiMethodResolved  = grMethodCallExpression.resolveMethod();//todo fix issue with methods not resolved in old idea versions
            if (psiMethodResolved != null) {
                methodCalls.add(new ResolvedMethodCall(psiMethodResolved,methodCallArguments));
            }
        }
        final Collection<GrArgumentLabelImpl> grArgLabels = PsiTreeUtil.findChildrenOfType(psiMethod, GrArgumentLabelImpl.class);
        for (GrArgumentLabelImpl grArgumentLabel : grArgLabels) {
            final PsiElement psiElement = grArgumentLabel.resolve();
            if (psiElement != null && psiElement instanceof PsiMethod) {
                final PsiMethod psiMethodResolved  = (PsiMethod) psiElement;
                methodCalls.add(new ResolvedMethodCall(psiMethodResolved,null));//todo resolve args for this scenario as well?
            }
        }
        final Collection<GrReferenceExpression> grReferenceExpressions = PsiTreeUtil.findChildrenOfType(psiMethod, GrReferenceExpression.class);
        for (GrReferenceExpression grReferenceExpression : grReferenceExpressions) {
            final PsiElement psiElement = grReferenceExpression.resolve();
            if (psiElement!= null && (isGroovy(psiElement.getLanguage()) && psiElement instanceof GrMethod  || !isGroovy(psiElement.getLanguage())  && psiElement instanceof PsiMethod) ) {
                methodCalls.add(new ResolvedMethodCall((PsiMethod) psiElement,null));//todo resolve args
            }
        }
        return methodCalls;
    }

    private static PsiType resolveOwnerType(PsiElement psiElement) {
        boolean dotAppeared = false;
        for(PsiElement prevSibling  = psiElement.getPrevSibling();prevSibling!=null;prevSibling=prevSibling.getPrevSibling()) {
            if(".".equals(prevSibling.getText())) {
                dotAppeared = true;
            }
            else if(dotAppeared && prevSibling instanceof GrReferenceExpression ) {
                return GroovyPsiTreeUtils.resolveType(prevSibling);
            }
        }
        return null;
    }

    public static PsiField resolveGrLeftHandExpressionAsField(PsiElement element) {
        if (!(element instanceof GrReferenceExpression)) {
            return null;
        }
        final GrReferenceExpression grExpr = (GrReferenceExpression) element;
        PsiElement parent = PsiTreeUtil.skipParentsOfType(grExpr, GrParenthesizedExpression.class);
        if (!(parent instanceof GrAssignmentExpression)) {
            return null;
        }
        final GrAssignmentExpression grAssignmentExpression = (GrAssignmentExpression) parent;
        final PsiReference reference = grAssignmentExpression.getLValue().getReference();
        final PsiElement possibleFieldElement = reference != null ? reference.resolve() : null;
        return possibleFieldElement == null || !(possibleFieldElement instanceof PsiField) ? null : (PsiField)possibleFieldElement ;
    }

}
