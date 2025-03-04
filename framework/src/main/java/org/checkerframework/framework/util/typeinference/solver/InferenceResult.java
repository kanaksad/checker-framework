package org.checkerframework.framework.util.typeinference.solver;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredTarget;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;

/**
 * Represents the result from inferring type arguments. InferenceResult is a map from: target type
 * variable to (inferred type or target).
 */
public class InferenceResult extends LinkedHashMap<TypeVariable, InferredValue> {
  private static final long serialVersionUID = 6911459752070485818L;

  /**
   * Returns the set of targets that still don't have an inferred argument.
   *
   * @return the set of targets that still don't have an inferred argument
   */
  public Set<TypeVariable> getRemainingTargets(
      Set<TypeVariable> allTargets, boolean inferredTypesOnly) {
    Set<TypeVariable> remainingTargets = new LinkedHashSet<>(allTargets);

    if (inferredTypesOnly) {

      for (TypeVariable target : keySet()) {
        if (this.get(target) instanceof InferredType) {
          remainingTargets.remove(target);
        }
      }

    } else {
      remainingTargets.removeAll(this.keySet());
    }

    return remainingTargets;
  }

  /**
   * Returns true if we have inferred a concrete type for all targets.
   *
   * @param targets type variables to check
   * @return true if we have inferred a concrete type for all targets
   */
  public boolean isComplete(Set<TypeVariable> targets) {
    for (TypeVariable target : targets) {
      InferredValue inferred = this.get(target);

      if (inferred == null || inferred instanceof InferredTarget) {
        return false;
      } else if (inferred instanceof InferredType
          && ((InferredType) inferred).type.getKind() == TypeKind.NULL) {
        // NullType is not a valid type argument, so continue looking for the correct type.
        return false;
      }
    }
    return this.keySet().containsAll(targets);
  }

  /**
   * If we had a set of inferred results, (e.g. T1 = T2, T2 = T3, T3 = String) propagate any results
   * we have (the above constraints become T1 = String, T2 = String, T3 = String)
   */
  public void resolveChainedTargets() {
    Map<TypeVariable, InferredValue> inferredTypes = new LinkedHashMap<>(this.size());

    // TODO: we can probably make this a bit more efficient
    boolean grew = true;
    while (grew) {
      grew = false;
      for (Map.Entry<TypeVariable, InferredValue> inferred : this.entrySet()) {
        TypeVariable target = inferred.getKey();
        InferredValue value = inferred.getValue();

        if (value instanceof InferredType) {
          inferredTypes.put(target, value);

        } else {
          InferredTarget currentTarget = (InferredTarget) value;
          InferredType equivalentType =
              (InferredType) inferredTypes.get(((InferredTarget) value).target);

          if (equivalentType != null) {
            grew = true;
            AnnotatedTypeMirror type = equivalentType.type.deepCopy();
            type.replaceAnnotations(currentTarget.additionalAnnotations);

            InferredType newConstraint = new InferredType(type);
            inferredTypes.put(currentTarget.target, newConstraint);
          }
        }
      }
    }

    this.putAll(inferredTypes);
  }

  public Map<TypeVariable, AnnotatedTypeMirror> toAtmMap() {
    Map<TypeVariable, AnnotatedTypeMirror> result = new LinkedHashMap<>(this.size());
    for (Map.Entry<TypeVariable, InferredValue> entry : this.entrySet()) {
      InferredValue inferredValue = entry.getValue();
      if (inferredValue instanceof InferredType) {
        result.put(entry.getKey(), ((InferredType) inferredValue).type);
      }
    }

    return result;
  }

  /**
   * Merges values in subordinate into this result, keeping the results form any type arguments that
   * were already contained by this InferenceResult.
   *
   * @param subordinate a result which we wish to merge into this result
   */
  public void mergeSubordinate(InferenceResult subordinate) {
    Set<TypeVariable> previousKeySet = new LinkedHashSet<>(this.keySet());
    Set<TypeVariable> remainingSubKeys = new LinkedHashSet<>(subordinate.keySet());
    remainingSubKeys.removeAll(keySet());

    for (TypeVariable target : previousKeySet) {
      mergeTarget(target, subordinate);
    }

    for (TypeVariable target : remainingSubKeys) {
      this.put(target, subordinate.get(target));
    }

    resolveChainedTargets();
  }

  /** Performs a merge for a specific target, we keep only results that lead to a concrete type. */
  protected @Nullable InferredType mergeTarget(TypeVariable target, InferenceResult subordinate) {
    InferredValue inferred = this.get(target);
    if (inferred instanceof InferredTarget) {
      InferredType newType = mergeTarget(((InferredTarget) inferred).target, subordinate);

      if (newType == null) {
        InferredValue subValue = subordinate.get(target);
        if (subValue instanceof InferredType) {
          this.put(target, subValue);
          return null;
        }
      } else {
        if (newType.type.getKind() == TypeKind.NULL) {
          // If the newType is null, then use the subordinate type, but with the
          // primary annotations on null.
          InferredValue subValue = subordinate.get(target);
          if (subValue instanceof InferredType) {
            AnnotatedTypeMirror copy = ((InferredType) subValue).type.deepCopy();
            copy.replaceAnnotations(newType.type.getPrimaryAnnotations());
            newType = new InferredType(copy);
          }
        }
        this.put(target, newType);
        return newType;
      }

      return null;
    } // else

    return (InferredType) inferred;
  }
}
