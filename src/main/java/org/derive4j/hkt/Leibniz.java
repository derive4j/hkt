package org.derive4j.hkt;

/**
 * Provide witnesses of equality between two types using Leibnizian equality.
 * Leibniz instances do not have any effect on values: they only expose a sometimes hidden fact:
 * that two types are the same and can be safely substituted with one another in any context (ie. type constructors).
 *
 * Leibniz instance are safe alternative to type casting
 * and are often use to implements generalized algebraic data types.
 *
 * @param <A> a type {@code A}.
 * @param <B> a type which is guaranteed to be same as {@code A}.
 * @see <a href="http://portal.acm.org/citation.cfm?id=583852.581494">Typing Dynamic Typing</a>
 * @see <a href="http://typelevel.org/blog/2014/09/20/higher_leibniz.html">Higher Leibniz</a>
 * @see <a href="https://github.com/ekmett/eq">Leibnizian type equality in Haskell</a>
 */
public abstract class Leibniz<A, B> implements __2<Leibniz.µ, A, B> {

  private Leibniz() {
  }

  /**
   * Equality is reflexive: a type is equal to itself.
   *
   * @param <A> a type
   * @return a Leibniz representing the reflexive equality.
   */
  public static <A> Leibniz<A, A> refl() {
    // The only possible implementation, the identity:
    return new Leibniz<A, A>() {
      @Override public <f> __<f, A> subst(final __<f, A> fa) {
        return fa;
      }
    };
  }

  /**
   * Recover the concrete type of a higher kinded Leibniz value.
   * The cast safety is guaranteed by the hkt type checker.
   *
   * @param hkLeibniz the higher kinded leibniz.
   * @param <A> a type {@code A}.
   * @param <B> a type which is guaranteed to be same as {@code A}.
   * @return the same leibniz, casted to the corresponding concrete type.
   */
  public static <A, B> Leibniz<A, B> ofHkt(__<__<µ, A>, B> hkLeibniz) {
    return (Leibniz<A, B>) hkLeibniz;
  }

  /**
   * Reify the higher kinded type equality: {@code __<__<µ, A>} =:= {@code Leibniz<A, B>}
   * that is guaranteed by the hkt type checker.
   *
   * @param <A> a type {@code A}.
   * @param <B> a type which is guaranteed to be same as {@code A}.
   * @return a leibniz instance witness of the type equality.
   */
  public static <A, B> Leibniz<__<__<µ, A>, B>, Leibniz<A, B>> hkt() {
    return (Leibniz) refl();
  }

  /**
   * Leibnizian equality states that two things are equal if you can substitute one for the other in all contexts.
   *
   * @param fa a term of containing {@code A}.
   * @param <f> type constructor context.
   * @return the input value with {@code A} substituted by {@code B} in its type.
   */
  public abstract <f> __<f, B> subst(__<f, A> fa);

  /**
   * If two things are equal you can convert one to the other.
   *
   * @param a a value to which apply the type substitution.
   * @return the same value, after type substitution.
   */
  public final B apply(A a) {
    return Coerce.ofHkt(subst(new Coerce<>(a))).uncoerce;
  }

  /**
   * Equality is transitive.
   *
   * @param that another Leibniz to compose with.
   * @param <C> left operand of the transitive type equality.
   * @return the composition of the leibniz instances.
   */
  public final <C> Leibniz<C, B> compose(Leibniz<C, A> that) {
    return new Leibniz<C, B>() {
      @Override public <f> __<f, B> subst(__<f, C> fa) {
        return Leibniz.this.subst(that.subst(fa));
      }
    };
  }

  /**
   * Equality is transitive.
   *
   * @param that another Leibniz to be composed with.
   * @param <C> right operand of the transitive type equality.
   * @return the composition of the leibniz instances.
   */
  public final <C> Leibniz<A, C> andThen(Leibniz<B, C> that) {
    return that.compose(this);
  }

  /**
   * Equality is symmetric.
   *
   * @return the type equality seen from the other side.
   */
  public final Leibniz<B, A> symm() {
    return Symm.ofHkt(subst(new Symm<>(refl()))).leib;
  }

  /**
   * The type equality can be lifted into any type constructor.
   *
   * @param <f> a type constructor witness.
   * @return the leibniz equality in the context of the specified type constructor.
   */
  public final <f> Leibniz<__<f, A>, __<f, B>> lift() {
    return Lift.ofHkt(subst(new Lift<>(Leibniz.<__<f, A>>refl()))).unlift;
  }

  /**
   * The type equality can be lifted into any type constructor, at any position.
   *
   * @param <f> a type constructor witness.
   * @param <C> the last type variable (not substituted).
   * @return the leibniz equality in the context of the specified type constructor.
   */
  public final <f, C> Leibniz<__<__<f, A>, C>, __<__<f, B>, C>> lift2() {
    return Lift2.ofHkt(subst(new Lift2<>(Leibniz.<__<__<f, A>, C>>refl()))).unlift2;
  }

  /**
   * Type equalities can be lifted into any type constructor, at any positions.
   *
   * @param cd a leibniz for last type variable of any type constructor.
   * @param <C> the last type variable before substitution.
   * @param <D> the last type variable after substitution.
   * @return a factory to lift the leibniz instances into any type constructor.
   */
  public final <C, D> Lift2Leibniz<A, C, B, D> lift2(Leibniz<C, D> cd) {
    return new Lift2Leibniz<A, C, B, D>(this, cd);
  }

  /**
   * The type equality can be lifted into any type constructor, at any position.
   *
   * @param <f> a type constructor witness.
   * @param <C> the before last type variable (not substituted).
   * @param <D> the last type variable (not substituted).
   * @return the leibniz equality in the context of the specified type constructor.
   */
  public final <f, C, D> Leibniz<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> lift3() {
    return Lift3.ofHkt(subst(new Lift3<>(Leibniz.<__<__<__<f, A>, C>, D>>refl()))).unlift3;
  }

  /**
   * Type equalities can be lifted into any type constructor, at any positions.
   *
   * @param cd a leibniz for before last type variable of any type constructor.
   * @param ef a leibniz for last type variable of any type constructor.
   * @param <C> the before last type variable before substitution.
   * @param <D> the before last type variable after substitution.
   * @param <E> the last type variable before substitution.
   * @param <F> the last type variable after substitution.
   * @return a factory to lift the leibniz instances into any type constructor.
   */
  public final <C, D, E, F> Lift3Leibniz<A, C, E, B, D, F> lift3(Leibniz<C, D> cd, Leibniz<E, F> ef) {
    return new Lift3Leibniz<A, C, E, B, D, F>(this, cd, ef);
  }

  /**
   * Serve as type constructor witness of Leibniz.
   */
  public enum µ {
  }

  /**
   * Type inference helper class:
   * allow to lift two leibniz instances into any type constructor.
   *
   * @param <A> the before last type variable before substitution.
   * @param <C> the last type variable before substitution.
   * @param <B> the before last type variable after substitution.
   * @param <D> the last type variable after substitution.
   */
  public static final class Lift2Leibniz<A, C, B, D> {

    private final Leibniz<A, B> ab;

    private final Leibniz<C, D> cd;

    Lift2Leibniz(Leibniz<A, B> ab, Leibniz<C, D> cd) {
      this.ab = ab;
      this.cd = cd;
    }

    /**
     * Lift two leibniz instances into a type constructor.
     *
     * @param <f> a type constructor witness.
     * @return the leibniz equalities in the context of the type constructor.
     */
    public <f> Leibniz<__<__<f, A>, C>, __<__<f, B>, D>> lift() {

      Leibniz<__<__<f, A>, C>, __<__<f, B>, C>> abLift = ab.lift2();
      Leibniz<__<__<f, B>, C>, __<__<f, B>, D>> cdLift = cd.lift();

      return new Leibniz<__<__<f, A>, C>, __<__<f, B>, D>>() {
        @Override public <f2> __<f2, __<__<f, B>, D>> subst(__<f2, __<__<f, A>, C>> fa) {
          return cdLift.subst(abLift.subst(fa));
        }
      };
    }
  }

  /**
   * Type inference helper class:
   * allow to lift three leibniz instances into any type constructor.
   *
   * @param <A> the antepenultimate type variable before substitution.
   * @param <C> the before last type variable before substitution.
   * @param <E> the last type variable before substitution.
   * @param <B> the antepenultimate type variable after substitution.
   * @param <D> the before last type variable after substitution.
   * @param <F> the last type variable after substitution.
   */
  public static final class Lift3Leibniz<A, C, E, B, D, F> {

    private final Leibniz<A, B> ab;

    private final Leibniz<C, D> cd;

    private final Leibniz<E, F> ef;

    Lift3Leibniz(Leibniz<A, B> ab, Leibniz<C, D> cd, Leibniz<E, F> ef) {
      this.ab = ab;
      this.cd = cd;
      this.ef = ef;
    }

    /**
     * Lift three leibniz instances into a type constructor.
     *
     * @param <f> a type constructor witness.
     * @return the leibniz equalities in the context of the type constructor.
     */
    public <f> Leibniz<__<__<__<f, A>, C>, E>, __<__<__<f, B>, D>, F>> lift() {

      Leibniz<__<__<__<f, A>, C>, E>, __<__<__<f, B>, C>, E>> abLift = ab.lift3();
      Leibniz<__<__<__<f, B>, C>, E>, __<__<__<f, B>, D>, E>> cdLift = cd.lift2();
      Leibniz<__<__<__<f, B>, D>, E>, __<__<__<f, B>, D>, F>> efLift = ef.lift();

      return new Leibniz<__<__<__<f, A>, C>, E>, __<__<__<f, B>, D>, F>>() {
        @Override public <f2> __<f2, __<__<__<f, B>, D>, F>> subst(__<f2, __<__<__<f, A>, C>, E>> fa) {
          return efLift.subst(cdLift.subst(abLift.subst(fa)));
        }
      };
    }
  }

  private static class Coerce<A> implements __<Coerce.µ, A> {

    final A uncoerce;

    Coerce(A uncoerce) {
      this.uncoerce = uncoerce;
    }

    static <A> Coerce<A> ofHkt(__<µ, A> hkId) {
      return (Coerce<A>) hkId;
    }

    enum µ {}
  }

  private static class Symm<A, B> implements __2<Symm.µ, A, B> {

    final Leibniz<B, A> leib;

    Symm(Leibniz<B, A> leib) {
      this.leib = leib;
    }

    static <A, B> Symm<A, B> ofHkt(__<__<µ, A>, B> hkSymm) {
      return (Symm<A, B>) hkSymm;
    }

    enum µ {}
  }

  private static class Lift<f, A, B> implements __3<Lift.µ, f, A, B> {

    final Leibniz<__<f, A>, __<f, B>> unlift;

    Lift(Leibniz<__<f, A>, __<f, B>> unlift) {
      this.unlift = unlift;
    }

    static <f, A, B> Lift<f, A, B> ofHkt(__<__<__<µ, f>, A>, B> hkLift) {
      return (Lift<f, A, B>) hkLift;
    }

    enum µ {}
  }

  private static class Lift2<f, C, A, B> implements __4<Lift2.µ, f, C, A, B> {

    final Leibniz<__<__<f, A>, C>, __<__<f, B>, C>> unlift2;

    Lift2(Leibniz<__<__<f, A>, C>, __<__<f, B>, C>> unlift2) {
      this.unlift2 = unlift2;
    }

    static <f, C, A, B> Lift2<f, C, A, B> ofHkt(__<__<__<__<µ, f>, C>, A>, B> hkLift2) {
      return (Lift2<f, C, A, B>) hkLift2;
    }

    enum µ {}
  }

  private static class Lift3<f, C, D, A, B> implements __5<Lift3.µ, f, C, D, A, B> {

    final Leibniz<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> unlift3;

    Lift3(Leibniz<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> unlift3) {
      this.unlift3 = unlift3;
    }

    static <f, C, D, A, B> Lift3<f, C, D, A, B> ofHkt(__<__<__<__<__<µ, f>, C>, D>, A>, B> hkLift3) {
      return (Lift3<f, C, D, A, B>) hkLift3;
    }

    enum µ {}
  }

}