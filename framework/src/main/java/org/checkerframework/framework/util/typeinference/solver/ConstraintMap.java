package org.checkerframework.framework.util.typeinference.solver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Equalities;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Subtypes;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Supertypes;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * ConstraintMap holds simplified versions of the TUConstraints for ALL type variable for which we
 * are inferring an argument. The ConstraintMap is edited on the fly as the various solvers work
 * (unlike the AF/TU Constraints which are immutable).
 *
 * <p>This really consists of these things:
 *
 * <ol>
 *   <li>a Map({@code target => constraints for target})
 *   <li>Methods to easily build up the constraints in the map
 *   <li>A getter for the constraints of individual targets.
 * </ol>
 *
 * Note: This class, along with TargetConstraints, uses a lot of mutable state and few
 * setters/getters be careful. This choice was made as it makes the resulting code more readable.
 */
public class ConstraintMap {

  private final Map<TypeVariable, TargetConstraints> targetToRecords = new LinkedHashMap<>();

  public ConstraintMap(Set<TypeVariable> targets) {
    for (TypeVariable target : targets) {
      targetToRecords.put(target, new TargetConstraints(target));
    }
  }

  public ConstraintMap(ConstraintMap toCopy) {
    this.targetToRecords.putAll(toCopy.targetToRecords);
  }

  /** Gets the equality, subtypes, and supertypes constraints for a particular target. */
  public TargetConstraints getConstraints(TypeVariable target) {
    return targetToRecords.get(target);
  }

  /**
   * Returns the set of all targets passed to the constructor of this constraint map (a target will
   * appear in this list whether or not it has any constraints added).
   *
   * @return the set of all targets passed to the constructor of this constraint map (a target will
   *     appear in this list whether or not it has any constraints added)
   */
  public Set<TypeVariable> getTargets() {
    return targetToRecords.keySet();
  }

  /**
   * Add a constraint indicating that the equivalent is equal to target in the given qualifier
   * hierarchies.
   */
  public void addTargetEquality(
      TypeVariable target, TypeVariable equivalent, AnnotationMirrorSet hierarchies) {
    Equalities equalities = targetToRecords.get(target).equalities;
    AnnotationMirrorSet equivalentTops =
        equalities.targets.computeIfAbsent(equivalent, __ -> new AnnotationMirrorSet());
    equivalentTops.addAll(hierarchies);
  }

  /**
   * Add a constraint indicating that target has primary annotations equal to the given annotations.
   */
  public void addPrimaryEqualities(
      TypeVariable target, QualifierHierarchy qualHierarchy, AnnotationMirrorSet annos) {
    Equalities equalities = targetToRecords.get(target).equalities;

    for (AnnotationMirror anno : annos) {
      AnnotationMirror top = qualHierarchy.getTopAnnotation(anno);
      if (!equalities.primaries.containsKey(top)) {
        equalities.primaries.put(top, anno);
      }
    }
  }

  /**
   * Add a constraint indicating that target is a supertype of subtype in the given qualifier
   * hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTargetSupertype(
      TypeVariable target, TypeVariable subtype, AnnotationMirrorSet hierarchies) {
    Supertypes supertypes = targetToRecords.get(target).supertypes;
    AnnotationMirrorSet supertypeTops =
        supertypes.targets.computeIfAbsent(subtype, __ -> new AnnotationMirrorSet());
    supertypeTops.addAll(hierarchies);
  }

  /**
   * Add a constraint indicating that target is a supertype of subtype in the given qualifier
   * hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTypeSupertype(
      TypeVariable target, AnnotatedTypeMirror subtype, AnnotationMirrorSet hierarchies) {
    Supertypes supertypes = targetToRecords.get(target).supertypes;
    AnnotationMirrorSet supertypeTops =
        supertypes.types.computeIfAbsent(subtype, __ -> new AnnotationMirrorSet());
    supertypeTops.addAll(hierarchies);
  }

  /**
   * Add a constraint indicating that target's primary annotations are subtypes of the given
   * annotations.
   */
  public void addPrimarySupertype(
      TypeVariable target, QualifierHierarchy qualHierarchy, AnnotationMirrorSet annos) {
    Supertypes supertypes = targetToRecords.get(target).supertypes;
    for (AnnotationMirror anno : annos) {
      AnnotationMirror top = qualHierarchy.getTopAnnotation(anno);
      AnnotationMirrorSet entries =
          supertypes.primaries.computeIfAbsent(top, __ -> new AnnotationMirrorSet());
      entries.add(anno);
    }
  }

  /**
   * Add a constraint indicating that target is a subtype of supertype in the given qualifier
   * hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTargetSubtype(
      TypeVariable target, TypeVariable supertype, AnnotationMirrorSet hierarchies) {
    Subtypes subtypes = targetToRecords.get(target).subtypes;
    AnnotationMirrorSet subtypesTops =
        subtypes.targets.computeIfAbsent(supertype, __ -> new AnnotationMirrorSet());
    subtypesTops.addAll(hierarchies);
  }

  /**
   * Add a constraint indicating that target is a subtype of supertype in the given qualifier
   * hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTypeSubtype(
      TypeVariable target, AnnotatedTypeMirror supertype, AnnotationMirrorSet hierarchies) {
    Subtypes subtypes = targetToRecords.get(target).subtypes;
    AnnotationMirrorSet subtypesTops =
        subtypes.types.computeIfAbsent(supertype, __ -> new AnnotationMirrorSet());
    subtypesTops.addAll(hierarchies);
  }

  /**
   * Add a constraint indicating that target's primary annotations are subtypes of the given
   * annotations.
   */
  public void addPrimarySubtypes(
      TypeVariable target, QualifierHierarchy qualHierarchy, AnnotationMirrorSet annos) {
    Subtypes subtypes = targetToRecords.get(target).subtypes;
    for (AnnotationMirror anno : annos) {
      AnnotationMirror top = qualHierarchy.getTopAnnotation(anno);
      AnnotationMirrorSet entries =
          subtypes.primaries.computeIfAbsent(top, __ -> new AnnotationMirrorSet());
      entries.add(anno);
    }
  }

  /**
   * Add a constraint indicating that target is equal to type in the given hierarchies.
   *
   * @param hierarchies a set of TOP annotations
   */
  public void addTypeEqualities(
      TypeVariable target, AnnotatedTypeMirror type, AnnotationMirrorSet hierarchies) {
    Equalities equalities = targetToRecords.get(target).equalities;
    AnnotationMirrorSet equalityTops =
        equalities.types.computeIfAbsent(type, __ -> new AnnotationMirrorSet());
    equalityTops.addAll(hierarchies);
  }
}
