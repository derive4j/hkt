package org.derive4j.hkt;

/**
 * Provide witnesses of equality between two types (propositional equality) using the Leibnizian equality definition.
 * TypeEq instances do not have any effect on values: they only expose a sometimes hidden fact:
 * that two types are the same and can be safely substituted with one another in any context (ie. type constructors).
 *
 * TypeEq instance are safe alternative to type casting and can be used to implements generalized algebraic data types.
 *
 * @param <A> a type {@link A}.
 * @param <B> a type {@link B} which is guaranteed to be same as {@link A}.
 * @see <a href="http://portal.acm.org/citation.cfm?id=583852.581494">Typing Dynamic Typing</a>
 * @see <a href="http://typelevel.org/blog/2014/09/20/higher_leibniz.html">Higher TypeEq</a>
 * @see <a href="https://github.com/ekmett/eq">Leibnizian type equality in Haskell</a>
 */
public abstract class TypeEq<A, B> implements __2<TypeEq.µ, A, B> {

  /** Serve as type constructor witness of TypeEq. */
  public enum µ {}

  private TypeEq() {
  }

  /**
   * Equality is reflexive: a type is equal to itself.
   *
   * @param <A> any type.
   * @return a TypeEq representing the reflexive equality.
   */
  public static <A> TypeEq<A, A> refl() {
    // The only possible implementation, the identity:
    return new TypeEq<A, A>() {
      @Override public <f> __<f, A> subst(final __<f, A> fa) {
        return fa;
      }
    };
  }

  /**
   * Recover the concrete type of a higher kinded TypeEq value.
   * The cast safety is guaranteed by the hkt type checker.
   *
   * @param hkTypeEq the higher kinded TypeEq.
   * @param <A> a type {@link A}.
   * @param <B> a type {@link B} which is guaranteed to be same as {@link A}.
   * @return the same TypeEq, casted to the corresponding concrete type.
   */
  public static <A, B> TypeEq<A, B> ofHkt(__<__<TypeEq.µ, A>, B> hkTypeEq) {
    return (TypeEq<A, B>) hkTypeEq;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<µ, A>, B} =:= {@code TypeEq<A, B}
   * </pre>
   * that is guaranteed by the hkt type checker.
   *
   * @param <A> a type {@link A}.
   * @param <B> a type {@link B} which is guaranteed to be same as {@link A}.
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  public static <A, B> TypeEq<__<__<TypeEq.µ, A>, B>, TypeEq<A, B>> hkt() {
    return (TypeEq) refl();
  }

  /**
   * Safe cast to __2. (guaranteed by the hkt type checker).
   */
  public static <f, A, B> __2<f, A, B> as__2(__<__<f, A>, B> fab) {
    return (__2<f, A, B>) fab;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __2<f, A, B>  =:= __<__<f, A>, B>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @param <A> before last type variable.
   * @param <B> last type variable.
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  public static <f, A, B> TypeEq<__<__<f, A>, B>, __2<f, A, B>> __2() {
    return (TypeEq) TypeEq.refl();
  }

  /**
   * Safe cast to __3. (guaranteed by the hkt type checker).
   */
  public static <f, A, B, C> __3<f, A, B, C> as__3(__<__<__<f, A>, B>, C> fabc) {
    return (__3<f, A, B, C>) fabc;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<__<f, A>, B>, C>}  =:= {@code __3<f, A, B, C>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @param <A> the antepenultimate type variable
   * @param <B> before last type variable.
   * @param <C> last type variable.
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  public static <f, A, B, C> TypeEq<__<__<__<f, A>, B>, C>, __3<f, A, B, C>> __3() {
    return (TypeEq) TypeEq.refl();
  }

  /**
   * Safe cast to __4. (guaranteed by the hkt type checker).
   */
  public static <f, A, B, C, D> __4<f, A, B, C, D> as__4(__<__<__<__<f, A>, B>, C>, D> fabcd) {
    return (__4<f, A, B, C, D>) fabcd;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<__<__<f, A>, B>, C>, D>}  =:= {@code __4<f, A, B, C, D>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  public static <f, A, B, C, D> TypeEq<__<__<__<__<f, A>, B>, C>, D>, __4<f, A, B, C, D>> __4() {
    return (TypeEq) TypeEq.refl();
  }

  /**
   * Safe cast to __5. (guaranteed by the hkt type checker).
   */
  public static <f, A, B, C, D, E> __5<f, A, B, C, D, E> as__5(__<__<__<__<__<f, A>, B>, C>, D>, E> fabcde) {
    return (__5<f, A, B, C, D, E>) fabcde;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<__<__<__<f, A>, B>, C>, D>, E>}  =:= {@code __5<f, A, B, C, D, E>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  public static <f, A, B, C, D, E> TypeEq<__<__<__<__<__<f, A>, B>, C>, D>, E>, __5<f, A, B, C, D, E>> __5() {
    return (TypeEq) TypeEq.refl();
  }

  /**
   * Safe cast to __6. (guaranteed by the hkt type checker).
   */
  public static <f, A, B, C, D, E, F> __6<f, A, B, C, D, E, F> as__6(__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F> fabcdef) {
    return (__6<f, A, B, C, D, E, F>) fabcdef;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>}  =:= {@code __6<f, A, B, C, D, E, F>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  public static <f, A, B, C, D, E, F> TypeEq<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, __6<f, A, B, C, D, E, F>> __6() {
    return (TypeEq) TypeEq.refl();
  }

  /**
   * Safe cast to __7. (guaranteed by the hkt type checker).
   */
  public static <f, A, B, C, D, E, F, G> __7<f, A, B, C, D, E, F, G> as__7(__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>
      fabcdefg) {
    return (__7<f, A, B, C, D, E, F, G>) fabcdefg;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>}  =:= {@code  __7<f, A, B, C, D, E, F, G>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  public static <f, A, B, C, D, E, F, G> TypeEq<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, __7<f, A, B, C, D, E, F, G>> __7() {
    return (TypeEq) TypeEq.refl();
  }


  /**
   * Safe cast to __8. (guaranteed by the hkt type checker).
   */
  public static <f, A, B, C, D, E, F, G, H> __8<f, A, B, C, D, E, F, G, H> as__8(__<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H> fabcdefgh) {
    return (__8<f, A, B, C, D, E, F, G, H>) fabcdefgh;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H>}  =:= {@code  __8<f, A, B, C, D, E, F, G, H>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  public static <f, A, B, C, D, E, F, G, H> TypeEq<__<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H>, __8<f, A, B, C, D, E, F, G, H>> __8() {
    return (TypeEq) TypeEq.refl();
  }


  /**
   * Safe cast to __9. (guaranteed by the hkt type checker).
   */
  public static <f, A, B, C, D, E, F, G, H, I> __9<f, A, B, C, D, E, F, G, H, I> as__9(__<__<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H>, I> fabcdefghi) {
    return (__9<f, A, B, C, D, E, F, G, H, I>) fabcdefghi;
  }

  /**
   * Reify the higher kinded type equality:<pre>
   * {@code __<__<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H>, I>}  =:= {@code  __9<f, A, B, C, D, E, F, G, H, I>}</pre>
   * that is guaranteed by the hkt type checker.
   *
   * @return a TypeEq instance witness of the type equality.
   */
  @SuppressWarnings("unchecked")
  public static <f, A, B, C, D, E, F, G, H, I> TypeEq<__<__<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H>, I>, __9<f, A, B, C, D, E, F, G, H, I>> __9() {
    return (TypeEq) TypeEq.refl();
  }

  /**
   * Leibnizian equality states that two things are equal if you can substitute one for the other in all contexts.
   *
   * @param fa a term whose type last parameter is {@link A}.
   * @param <f> type constructor witness of {@code fa} type.
   * @return the input value with {@link A} substituted by {@link B} in its type.
   */
  public abstract <f> __<f, B> subst(__<f, A> fa);

  /**
   * If two things are equal you can convert one to the other (safe cast).
   *
   * @param a a value of type {@link A} that will be coerced into type {@link B}.
   * @return the same value, after type coercion.
   */
  public final B coerce(A a) {
    return Identity.ofHkt(subst(new Identity<>(a))).runIdentity;
  }

  /**
   * Equality is transitive.
   *
   * @param that another TypeEq to compose with.
   * @param <C> left operand of the transitive type equality.
   * @return the composition of the TypeEq instances.
   */
  public final <C> TypeEq<C, B> compose(TypeEq<C, A> that) {
    return new TypeEq<C, B>() {
      @Override public <f> __<f, B> subst(__<f, C> fa) {
        return TypeEq.this.subst(that.subst(fa));
      }
    };
  }

  /**
   * Equality is transitive.
   *
   * @param that another TypeEq to be composed with.
   * @param <C> right operand of the transitive type equality.
   * @return the composition of the TypeEq instances.
   */
  public final <C> TypeEq<A, C> andThen(TypeEq<B, C> that) {
    return that.compose(this);
  }

  /**
   * Equality is symmetric.
   *
   * @return the type equality seen from the other side.
   */
  public final TypeEq<B, A> symm() {
    return Symm.ofHkt(subst(new Symm<>(refl()))).typeEq;
  }

  /**
   * The type equality can be lifted into any type constructor.
   *
   * @param <f> a type constructor witness.
   * @return the type equality in the context of the specified type constructor.
   */
  public final <f> TypeEq<__<f, A>, __<f, B>> lift() {
    return Lift.ofHkt(subst(new Lift<>(TypeEq.<__<f, A>>refl()))).unlift;
  }

  /**
   * The type equality can be lifted into any type constructor, at any position.
   *
   * @param <f> a type constructor witness.
   * @param <C> the last type variable (not substituted).
   * @return the type equality in the context of the specified type constructor.
   */
  public final <f, C> TypeEq<__<__<f, A>, C>, __<__<f, B>, C>> lift2() {
    return Lift2.ofHkt(subst(new Lift2<>(TypeEq.<__<__<f, A>, C>>refl()))).unlift2;
  }

  /**
   * Type equalities can be lifted into any type constructor, at any positions.
   *
   * @param cd a type equality witness for last type variable of any type constructor.
   * @param <C> the last type variable before substitution.
   * @param <D> the last type variable after substitution.
   * @return a factory to lift the TypeEq instances into any type constructor.
   */
  public final <C, D> Lift2TypeEq<A, C, B, D> lift2(TypeEq<C, D> cd) {
    return new Lift2TypeEq<A, C, B, D>(this, cd);
  }

  /**
   * The type equality can be lifted into any type constructor, at any position.
   *
   * @param <f> a type constructor witness.
   * @param <C> the before last type variable (not substituted).
   * @param <D> the last type variable (not substituted).
   * @return the type equality in the context of the specified type constructor.
   */
  public final <f, C, D> TypeEq<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> lift3() {
    return Lift3.ofHkt(subst(new Lift3<>(TypeEq.<__<__<__<f, A>, C>, D>>refl()))).unlift3;
  }

  /**
   * Type equalities can be lifted into any type constructor, at any positions.
   *
   * @param cd a type equality witness for before last type variable of any type constructor.
   * @param ef a type equality witness for last type variable of any type constructor.
   * @param <C> the before last type variable before substitution.
   * @param <D> the before last type variable after substitution.
   * @param <E> the last type variable before substitution.
   * @param <F> the last type variable after substitution.
   * @return a factory to lift the TypeEq instances into any type constructor.
   */
  public final <C, D, E, F> Lift3TypeEq<A, C, E, B, D, F> lift3(TypeEq<C, D> cd, TypeEq<E, F> ef) {
    return new Lift3TypeEq<A, C, E, B, D, F>(this, cd, ef);
  }

  /**
   * Type inference helper class:
   * allow to lift two TypeEq instances into any type constructor.
   *
   * @param <A> the before last type variable before substitution.
   * @param <C> the last type variable before substitution.
   * @param <B> the before last type variable after substitution.
   * @param <D> the last type variable after substitution.
   */
  public static final class Lift2TypeEq<A, C, B, D> {

    private final TypeEq<A, B> ab;

    private final TypeEq<C, D> cd;

    Lift2TypeEq(TypeEq<A, B> ab, TypeEq<C, D> cd) {
      this.ab = ab;
      this.cd = cd;
    }

    /**
     * Lift two TypeEq instances into a type constructor.
     *
     * @param <f> a type constructor witness.
     * @return the type equalities in the context of the type constructor.
     */
    public <f> TypeEq<__<__<f, A>, C>, __<__<f, B>, D>> lift() {

      TypeEq<__<__<f, A>, C>, __<__<f, B>, C>> abLift = ab.lift2();
      TypeEq<__<__<f, B>, C>, __<__<f, B>, D>> cdLift = cd.lift();

      return new TypeEq<__<__<f, A>, C>, __<__<f, B>, D>>() {
        @Override public <f2> __<f2, __<__<f, B>, D>> subst(__<f2, __<__<f, A>, C>> fa) {
          return cdLift.subst(abLift.subst(fa));
        }
      };
    }
  }

  /**
   * Type inference helper class:
   * allow to lift three TypeEq instances into any type constructor.
   *
   * @param <A> the antepenultimate type variable before substitution.
   * @param <C> the before last type variable before substitution.
   * @param <E> the last type variable before substitution.
   * @param <B> the antepenultimate type variable after substitution.
   * @param <D> the before last type variable after substitution.
   * @param <F> the last type variable after substitution.
   */
  public static final class Lift3TypeEq<A, C, E, B, D, F> {

    private final TypeEq<A, B> ab;

    private final TypeEq<C, D> cd;

    private final TypeEq<E, F> ef;

    Lift3TypeEq(TypeEq<A, B> ab, TypeEq<C, D> cd, TypeEq<E, F> ef) {
      this.ab = ab;
      this.cd = cd;
      this.ef = ef;
    }

    /**
     * Lift three TypeEq instances into a type constructor.
     *
     * @param <f> a type constructor witness.
     * @return the type equalities in the context of the type constructor.
     */
    public <f> TypeEq<__<__<__<f, A>, C>, E>, __<__<__<f, B>, D>, F>> lift() {

      TypeEq<__<__<__<f, A>, C>, E>, __<__<__<f, B>, C>, E>> abLift = ab.lift3();
      TypeEq<__<__<__<f, B>, C>, E>, __<__<__<f, B>, D>, E>> cdLift = cd.lift2();
      TypeEq<__<__<__<f, B>, D>, E>, __<__<__<f, B>, D>, F>> efLift = ef.lift();

      return new TypeEq<__<__<__<f, A>, C>, E>, __<__<__<f, B>, D>, F>>() {
        @Override public <f2> __<f2, __<__<__<f, B>, D>, F>> subst(__<f2, __<__<__<f, A>, C>, E>> fa) {
          return efLift.subst(cdLift.subst(abLift.subst(fa)));
        }
      };
    }
  }

  private static class Identity<A> implements __<Identity.µ, A> {

    final A runIdentity;

    Identity(A runIdentity) {
      this.runIdentity = runIdentity;
    }

    static <A> Identity<A> ofHkt(__<µ, A> hkId) {
      return (Identity<A>) hkId;
    }

    enum µ {}
  }

  private static class Symm<A, B> implements __2<Symm.µ, A, B> {

    final TypeEq<B, A> typeEq;

    Symm(TypeEq<B, A> typeEq) {
      this.typeEq = typeEq;
    }

    static <A, B> Symm<A, B> ofHkt(__<__<µ, A>, B> hkSymm) {
      return (Symm<A, B>) hkSymm;
    }

    enum µ {}
  }

  private static class Lift<f, A, B> implements __3<Lift.µ, f, A, B> {

    final TypeEq<__<f, A>, __<f, B>> unlift;

    Lift(TypeEq<__<f, A>, __<f, B>> unlift) {
      this.unlift = unlift;
    }

    static <f, A, B> Lift<f, A, B> ofHkt(__<__<__<µ, f>, A>, B> hkLift) {
      return (Lift<f, A, B>) hkLift;
    }

    enum µ {}
  }

  private static class Lift2<f, C, A, B> implements __4<Lift2.µ, f, C, A, B> {

    final TypeEq<__<__<f, A>, C>, __<__<f, B>, C>> unlift2;

    Lift2(TypeEq<__<__<f, A>, C>, __<__<f, B>, C>> unlift2) {
      this.unlift2 = unlift2;
    }

    static <f, C, A, B> Lift2<f, C, A, B> ofHkt(__<__<__<__<µ, f>, C>, A>, B> hkLift2) {
      return (Lift2<f, C, A, B>) hkLift2;
    }

    enum µ {}
  }

  private static class Lift3<f, C, D, A, B> implements __5<Lift3.µ, f, C, D, A, B> {

    final TypeEq<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> unlift3;

    Lift3(TypeEq<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> unlift3) {
      this.unlift3 = unlift3;
    }

    static <f, C, D, A, B> Lift3<f, C, D, A, B> ofHkt(__<__<__<__<__<µ, f>, C>, D>, A>, B> hkLift3) {
      return (Lift3<f, C, D, A, B>) hkLift3;
    }

    enum µ {}
  }

}