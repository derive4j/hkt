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
@SuppressWarnings("Convert2MethodRef")
public interface TypeEq<A, B> extends  __2<TypeEq.µ, A, B> {
  /** Type constructor witness of TypeEq. */
  enum µ {}

  /**
   * Leibnizian equality states that two things are equal if you can substitute one for the other in all contexts.
   *
   * @param fa a term whose type last parameter is {@link A}.
   * @param <f> type constructor witness of {@code fa} type.
   * @return the input value with {@link A} substituted by {@link B} in its type.
   */
  <f> __<f, B> subst(__<f, A> fa);

  /**
   * If two things are equal you can convert one to the other (safe cast).
   *
   * @param a a value of type {@link A} that will be coerced into type {@link B}.
   * @return the same value, after type coercion.
   */
  default B coerce(A a) {
    return Id.narrow(subst((Id<A>) () -> a)).__();
  }

  /**
   * Equality is transitive.
   *
   * @param that another TypeEq to compose with.
   * @param <C> left operand of the transitive type equality.
   * @return the composition of the TypeEq instances.
   */
  default <C> TypeEq<C, B> compose(TypeEq<C, A> that) {
    return TypeEq.ofHkt(subst(that));
  }

  /**
   * Equality is transitive.
   *
   * @param that another TypeEq to be composed with.
   * @param <C> right operand of the transitive type equality.
   * @return the composition of the TypeEq instances.
   */
  default <C> TypeEq<A, C> andThen(TypeEq<B, C> that) {
    return that.compose(this);
  }

  /**
   * Equality is symmetric.
   *
   * @return the type equality seen from the other side.
   */
  default TypeEq<B, A> symm() {
    final Symm<A, A> symm = () -> refl();

    return Symm.narrow(subst(symm)).__();
  }

  /**
   * The type equality can be lifted into any type constructor.
   *
   * @param <f> a type constructor witness.
   * @return the type equality in the context of the specified type constructor.
   */
  default <f> TypeEq<__<f, A>, __<f, B>> lift() {
    final Lift<f, A, A> _refl = () -> refl();

    return Lift.narrow(subst(_refl)).__();
  }

  /**
   * The type equality can be lifted into any type constructor, at any position.
   *
   * @param <f> a type constructor witness.
   * @param <C> the last type variable (not substituted).
   * @return the type equality in the context of the specified type constructor.
   */
  default <f, C> TypeEq<__<__<f, A>, C>, __<__<f, B>, C>> lift2() {
    return lift2(refl());
  }

  /**
   * Type equalities can be lifted into any type constructor, at any positions.
   *
   * @param cd a type equality witness for last type variable of any type constructor.
   * @param <C> the last type variable before substitution.
   * @param <D> the last type variable after substitution.
   * @return a factory to lift the TypeEq instances into any type constructor.
   */
  default <f, C, D> TypeEq<__<__<f, A>, C>, __<__<f, B>, D>> lift2(TypeEq<C, D> cd) {
    final Lift2_1<f, A, C, A> _refl = () -> refl();

    final Lift2_1<f, A, C, B> lift2_1 = Lift2_1.narrow(subst(_refl));

    final Lift2<f, A, C, B, C> lift2 = () -> lift2_1.__();

    return Lift2.narrow(cd.subst(lift2)).__();
  }

  /**
   * The type equality can be lifted into any type constructor, at any position.
   *
   * @param <f> a type constructor witness.
   * @param <C> the before last type variable (not substituted).
   * @param <D> the last type variable (not substituted).
   * @return the type equality in the context of the specified type constructor.
   */
  default <f, C, D> TypeEq<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> lift3() {
    return lift3(refl(), refl());
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
  default <f, C, D, E, F> TypeEq<__<__<__<f, A>, C>, E>, __<__<__<f, B>, D>, F>> lift3(TypeEq<C, D> cd
    , TypeEq<E, F> ef) {
    final Lift3_1<f, A, C, E, A> _refl = () -> refl();

    final Lift3_1<f, A, C, E, B> lift3_1 = Lift3_1.narrow(subst(_refl));

    final Lift3_2<f, A, C, E, B, C> lift3_2 = () -> lift3_1.__();

    final Lift3_2<f, A, C, E, B, D> lift3_2_1 = Lift3_2.narrow(cd.subst(lift3_2));

    final Lift3<f, A, C, E, B, D, E> lift3 = () -> lift3_2_1.__();

    return Lift3.narrow(ef.subst(lift3)).__();
  }

  /**
   * Equality is reflexive: a type is equal to itself.
   *
   * @param <T> any type.
   * @return a TypeEq representing the reflexive equality.
   */
  static <T> TypeEq<T, T> refl() { return HktId::id; }

  /**
   * Recover the concrete type of a higher kinded TypeEq value.
   * The cast safety is guaranteed by the hkt type checker.
   *
   * @param hkTypeEq the higher kinded TypeEq.
   * @param <A> a type {@link A}.
   * @param <B> a type {@link B} which is guaranteed to be same as {@link A}.
   * @return the same TypeEq, casted to the corresponding concrete type.
   */
  static <A, B> TypeEq<A, B> ofHkt(__<__<TypeEq.µ, A>, B> hkTypeEq) {
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
  static <A, B> TypeEq<__<__<TypeEq.µ, A>, B>, TypeEq<A, B>> hkt() {
    return (TypeEq) TypeEq.refl();
  }

  /**
   * Safe cast to __2. (guaranteed by the hkt type checker).
   */
  static <f, A, B> __2<f, A, B> as__2(__<__<f, A>, B> fab) {
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
  static <f, A, B, C, D, E, F> __6<f, A, B, C, D, E, F> as__6(__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F> fabcdef) {
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
  static <f, A, B, C, D, E, F> TypeEq<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, __6<f, A, B, C, D, E, F>> __6() {
    return (TypeEq) TypeEq.refl();
  }

  /**
   * Safe cast to __7. (guaranteed by the hkt type checker).
   */
  static <f, A, B, C, D, E, F, G> __7<f, A, B, C, D, E, F, G> as__7(__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>
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
  static <f, A, B, C, D, E, F, G> TypeEq<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, __7<f, A, B, C, D, E, F, G>> __7() {
    return (TypeEq) TypeEq.refl();
  }


  /**
   * Safe cast to __8. (guaranteed by the hkt type checker).
   */
  static <f, A, B, C, D, E, F, G, H> __8<f, A, B, C, D, E, F, G, H> as__8(__<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H> fabcdefgh) {
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
  static <f, A, B, C, D, E, F, G, H> TypeEq<__<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H>, __8<f, A, B, C, D, E, F, G, H>> __8() {
    return (TypeEq) TypeEq.refl();
  }


  /**
   * Safe cast to __9. (guaranteed by the hkt type checker).
   */
  static <f, A, B, C, D, E, F, G, H, I> __9<f, A, B, C, D, E, F, G, H, I> as__9(__<__<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H>, I> fabcdefghi) {
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
  static <f, A, B, C, D, E, F, G, H, I> TypeEq<__<__<__<__<__<__<__<__<__<f, A>, B>, C>, D>, E>, F>, G>, H>, I>, __9<f, A, B, C, D, E, F, G, H, I>> __9() {
    return (TypeEq) TypeEq.refl();
  }
}

// ################ Type inference helpers

interface HktId {
  static <f, T> __<f, T> id(__<f, T> ft) { return ft; }
}

interface Id<A> extends __<Id.µ, A> {
  enum µ {}

  A __();

  static <A> Id<A> narrow(__<Id.µ, A> a) { return (Id<A>) a; }
}

interface Symm<A, B> extends __2<Symm.µ, A, B> {
  enum µ {}

  TypeEq<B, A> __();

  static <f, A, B> Symm<A, B> narrow(__<__<Symm.µ, A>, B> _lift) {
    return (Symm<A, B>) _lift;
  }
}

interface Lift<f, A, B> extends __3<Lift.µ, f, A, B> {
  enum µ {}

  TypeEq<__<f, A>, __<f, B>> __();

  static <f, A, X> Lift<f, A, X> narrow(__<__<__<Lift.µ, f>, A>, X> _lift) {
    return (Lift<f, A, X>) _lift;
  }
}

interface Lift2_1<f, A, B, X> extends __4<Lift2_1.µ, f, A, B, X> {
  enum µ {}

  TypeEq<__<__<f, A>, B>, __<__<f, X>, B>> __();

  static <f, A, B, X> Lift2_1<f, A, B, X> narrow(__<__<__<__<Lift2_1.µ, f>, A>, B>, X> lift1) {
    return (Lift2_1<f, A, B, X>) lift1;
  }
}

interface Lift2<f, A, B, C, X> extends __5<Lift2.µ, f, A, B, C, X>  {
  enum µ {}

  TypeEq<__<__<f, A>, B>, __<__<f, C>, X>> __();

  static <f, A, B, C, X> Lift2<f, A, B, C, X> narrow(__<__<__<__<__<Lift2.µ, f>, A>, B>, C>, X> lift2) {
    return (Lift2<f, A, B, C, X>) lift2;
  }
}

interface Lift3_1<f, A, B, C, X> extends __5<Lift3_1.µ, f, A, B, C, X> {
  enum µ {}

  TypeEq<__<__<__<f, A>, B>, C>, __<__<__<f, X>, B>, C>> __();

  static <f, A, B, C, X> Lift3_1<f, A, B, C, X> narrow(__<__<__<__<__<Lift3_1.µ, f>, A>, B>, C>, X> lift3) {
    return (Lift3_1<f, A, B, C, X>) lift3;
  }
}

interface Lift3_2<f, A, B, C, D, X> extends __6<Lift3_2.µ, f, A, B, C, D, X> {
  enum µ {}

  TypeEq<__<__<__<f, A>, B>, C>, __<__<__<f, D>, X>, C>> __();

  static <f, A, B, C, D, X> Lift3_2<f, A, B, C, D, X> narrow(__<__<__<__<__<__<Lift3_2.µ, f>, A>, B>, C>, D>, X> lift3) {
    return (Lift3_2<f, A, B, C, D, X>) lift3;
  }
}

interface Lift3<f, A, B, C, D, E, X> extends __7<Lift3.µ, f, A, B, C, D, E, X> {
  enum µ {}

  TypeEq<__<__<__<f, A>, B>, C>, __<__<__<f, D>, E>, X>> __();

  static <f, A, B, C, D, E, X> Lift3<f, A, B, C, D, E, X> narrow(__<__<__<__<__<__<__<Lift3.µ, f>, A>, B>, C>, D>, E>, X> lift3) {
    return (Lift3<f, A, B, C, D, E, X>) lift3;
  }
}
