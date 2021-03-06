package org.junit.internal.runners.rules;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runners.model.FrameworkMember;
import org.junit.runners.model.TestClass;

/**
 * A RuleFieldValidator validates the rule fields of a
 * {@link org.junit.runners.model.TestClass}. All reasons for rejecting the
 * {@code TestClass} are written to a list of errors.
 *
 * There are four slightly different validators. The {@link #CLASS_RULE_VALIDATOR}
 * validates fields with a {@link ClassRule} annotation and the
 * {@link #RULE_VALIDATOR} validates fields with a {@link Rule} annotation.
 *
 * The {@link #CLASS_RULE_METHOD_VALIDATOR}
 * validates methods with a {@link ClassRule} annotation and the
 * {@link #RULE_METHOD_VALIDATOR} validates methods with a {@link Rule} annotation.
 */
public enum RuleFieldValidator {
    /**
     * Validates fields with a {@link ClassRule} annotation.
     */
    CLASS_RULE_VALIDATOR(ClassRule.class, false, true),
    /**
     * Validates fields with a {@link Rule} annotation.
     */
    RULE_VALIDATOR(Rule.class, false, false),
    /**
     * Validates methods with a {@link ClassRule} annotation.
     */
    CLASS_RULE_METHOD_VALIDATOR(ClassRule.class, true, true),
    /**
     * Validates methods with a {@link Rule} annotation.
     */
    RULE_METHOD_VALIDATOR(Rule.class, true, false);

    private final Class<? extends Annotation> annotation;

    private final boolean staticMembers;
    private final boolean methods;

    private RuleFieldValidator(Class<? extends Annotation> annotation,
            boolean methods, boolean staticMembers) {
        this.annotation = annotation;
        this.staticMembers = staticMembers;
        this.methods = methods;
    }

    /**
     * Validate the {@link org.junit.runners.model.TestClass} and adds reasons
     * for rejecting the class to a list of errors.
     *
     * @param target the {@code TestClass} to validate.
     * @param errors the list of errors.
     */
    public void validate(TestClass target, List<Throwable> errors) {
        List<? extends FrameworkMember<?>> members = methods ? target.getAnnotatedMethods(annotation)
                : target.getAnnotatedFields(annotation);

        for (FrameworkMember<?> each : members) {
            validateMember(each, errors);
        }
    }

    private void validateMember(FrameworkMember<?> member, List<Throwable> errors) {
        validatePublicClass(member, errors);
        validateStatic(member, errors);
        validatePublic(member, errors);
        validateTestRuleOrMethodRule(member, errors);
    }
    
    private void validatePublicClass(FrameworkMember<?> member, List<Throwable> errors) {
        if (staticMembers && !isDeclaringClassPublic(member)) {
            addError(errors, member, " must be declared in a public class.");
        }
    }

    private void validateStatic(FrameworkMember<?> member, List<Throwable> errors) {
        if (staticMembers && !member.isStatic()) {
            addError(errors, member, "must be static.");
        }
        if (!staticMembers && member.isStatic()) {
            addError(errors, member, "must not be static or it has to be annotated with @ClassRule.");
        }
    }

    private void validatePublic(FrameworkMember<?> member, List<Throwable> errors) {
        if (!member.isPublic()) {
            addError(errors, member, "must be public.");
        }
    }

    private void validateTestRuleOrMethodRule(FrameworkMember<?> member,
            List<Throwable> errors) {
        if (!isMethodRule(member) && !isTestRule(member)) {
            addError(errors, member, methods ?
                    "must return an implementation of MethodRule or TestRule." :
                    "must implement MethodRule or TestRule.");
        }
    }

    private boolean isDeclaringClassPublic(FrameworkMember<?> member) {
        return Modifier.isPublic(member.getDeclaringClass().getModifiers());
    }

    private boolean isTestRule(FrameworkMember<?> member) {
        return TestRule.class.isAssignableFrom(member.getType());
    }

    private boolean isMethodRule(FrameworkMember<?> member) {
        return MethodRule.class.isAssignableFrom(member.getType());
    }

    private void addError(List<Throwable> errors, FrameworkMember<?> member,
            String suffix) {
        String message = "The @" + annotation.getSimpleName() + " '"
                + member.getName() + "' " + suffix;
        errors.add(new Exception(message));
    }
}
