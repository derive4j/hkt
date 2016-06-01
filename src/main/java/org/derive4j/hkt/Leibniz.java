package org.derive4j.hkt;

public abstract class Leibniz<A, B> implements __2<Leibniz.µ, A, B> {

  private Leibniz() {}

  public static <A> Leibniz<A, A> refl() {
    // The only possible implementation :
    return new Leibniz<A, A>() {
      @Override public <f> __<f, A> subst(final __<f, A> fa) {
        return fa;
      }
    };
  }

  public static <A, B> Leibniz<A, B> ofHkt(__<__<µ, A>, B> hkLeibniz) {
    return (Leibniz) hkLeibniz;
  }

  public abstract <f> __<f, B> subst(__<f, A> fa);

  public final B apply(A a) {
    return Id.ofHkt(subst(new Id<>(a))).run;
  }

  public final <C> Leibniz<C, B> compose(Leibniz<C, A> that) {
    return new Leibniz<C, B>() {
      @Override public <f> __<f, B> subst(__<f, C> fa) {
        return Leibniz.this.subst(that.subst(fa));
      }
    };
  }

  public final <C> Leibniz<A, C> andThen(Leibniz<B, C> that) {
    return that.compose(this);
  }

  public final Leibniz<B, A> symm() {
    return Symm.ofHkt(subst(new Symm<>(refl()))).leib;
  }

  public final <f> Leibniz<__<f, A>, __<f, B>> lift() {
    return Lift.ofHkt(subst(new Lift<>(Leibniz.<__<f, A>>refl()))).leib;
  }

  public enum µ {}

  private static class Id<A> implements __<Id.µ, A> {

    final A run;

    Id(A run) {
      this.run = run;
    }

    static <A> Id<A> ofHkt(__<µ, A> hkId) {
      return (Id<A>) hkId;
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

  private static class Lift<f, fA, fB, A, B> implements __5<Lift.µ, f, fA, fB, A, B> {

    final Leibniz<__<fA, A>, __<fB, B>> leib;

    Lift(Leibniz<__<fA, A>, __<fB, B>> leib) {
      this.leib = leib;
    }

    static <f, fA, fB, A, B> Lift<f, fA, fB, A, B> ofHkt(__<__<__<__<__<µ, f>, fA>, fB>, A>, B> hkLift) {
      return (Lift<f, fA, fB, A, B>) hkLift;
    }

    enum µ {}
  }

}