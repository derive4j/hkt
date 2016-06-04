package org.derive4j.hkt;

public abstract class Leibniz<A, B> implements __2<Leibniz.µ, A, B> {

  private Leibniz() {
  }

  public static <A> Leibniz<A, A> refl() {
    // The only possible implementation :
    return new Leibniz<A, A>() {
      @Override public <f> __<f, A> subst(final __<f, A> fa) {
        return fa;
      }
    };
  }

  public static <A, B> Leibniz<A, B> ofHkt(__<__<µ, A>, B> hkLeibniz) {
    return (Leibniz<A, B>) hkLeibniz;
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

  public final <f, C> Leibniz<__<__<f, A>, C>, __<__<f, B>, C>> lift2() {
    return Lift2.ofHkt(subst(new Lift2<>(Leibniz.<__<__<f, A>, C>>refl()))).leib;
  }
  public final <C, D> Lift2ForAllF<A, C, B, D> lift2(Leibniz<C, D> cd) {
    return new Lift2ForAllF<A, C, B, D>() {
      @Override public <f> Leibniz<__<__<f, A>, C>, __<__<f, B>, D>> f() {

        Leibniz<__<__<f, A>, C>, __<__<f, B>, C>> abLift = lift2();
        Leibniz<__<__<f, B>, C>, __<__<f, B>, D>> cdLift = cd.lift();

        return new Leibniz<__<__<f, A>, C>, __<__<f, B>, D>>() {
          @Override public <f2> __<f2, __<__<f, B>, D>> subst(__<f2, __<__<f, A>, C>> fa) {
            return cdLift.subst(abLift.subst(fa));
          }
        };
      }
    };

  }
  public final <f, C, D> Leibniz<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> lift3() {
    return Lift3.ofHkt(subst(new Lift3<>(Leibniz.<__<__<__<f, A>, C>, D>>refl()))).leib;
  }
  public final <C, D, E, F> Lift3ForAllF<A, C, E, B, D, F> lift3(Leibniz<C, D> cd, Leibniz<E, F> ef) {

    return new Lift3ForAllF<A, C, E, B, D, F>() {
      @Override public <f> Leibniz<__<__<__<f, A>, C>, E>, __<__<__<f, B>, D>, F>> f() {
        Leibniz<__<__<__<f, A>, C>, E>, __<__<__<f, B>, C>, E>> abLift = lift3();
        Leibniz<__<__<__<f, B>, C>, E>, __<__<__<f, B>, D>, E>> cdLift = cd.lift2();
        Leibniz<__<__<__<f, B>, D>, E>, __<__<__<f, B>, D>, F>> efLift = ef.lift();

        return new Leibniz<__<__<__<f, A>, C>, E>, __<__<__<f, B>, D>, F>>() {
          @Override public <f2> __<f2, __<__<__<f, B>, D>, F>> subst(__<f2, __<__<__<f, A>, C>, E>> fa) {
            return efLift.subst(cdLift.subst(abLift.subst(fa)));
          }
        };
      }
    };

  }
  
  public static <f, A, B> Leibniz<A, B> lower(Leibniz<__<f,A>, __<f,B>> a) {
    return Lower.ofHkt(a.subst(new Lower(new Lower2<f, A, A, __<f,A>, __<f,A>>(Leibniz.<A>refl(), Leibniz.<__<f,A>>refl(), Leibniz.<__<f,A>>refl())))).lower2.leib;
  }

  public enum µ {}

  public interface Lift2ForAllF<A, C, B, D> {
    <f> Leibniz<__<__<f, A>, C>, __<__<f, B>, D>> f();
  }

  public interface Lift3ForAllF<A, C, E, B, D, F> {
    <f> Leibniz<__<__<__<f, A>, C>, E>, __<__<__<f, B>, D>, F>> f();
  }

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

  private static class Lift<f, A, B> implements __3<Lift.µ, f, A, B> {

    final Leibniz<__<f, A>, __<f, B>> leib;

    Lift(Leibniz<__<f, A>, __<f, B>> leib) {
      this.leib = leib;
    }

    static <f, A, B> Lift<f, A, B> ofHkt(__<__<__<µ, f>, A>, B> hkLift) {
      return (Lift<f, A, B>) hkLift;
    }

    enum µ {}
  }

  private static class Lift2<f, C, A, B> implements __4<Lift2.µ, f, C, A, B> {

    final Leibniz<__<__<f, A>, C>, __<__<f, B>, C>> leib;

    Lift2(Leibniz<__<__<f, A>, C>, __<__<f, B>, C>> leib) {
      this.leib = leib;
    }

    static <f, C, A, B> Lift2<f, C, A, B> ofHkt(__<__<__<__<µ, f>, C>, A>, B> hkLift) {
      return (Lift2<f, C, A, B>) hkLift;
    }

    enum µ {}
  }

  private static class Lift3<f, C, D, A, B> implements __5<Lift3.µ, f, C, D, A, B> {

    final Leibniz<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> leib;

    Lift3(Leibniz<__<__<__<f, A>, C>, D>, __<__<__<f, B>, C>, D>> leib) {
      this.leib = leib;
    }

    static <f, C, D, A, B> Lift3<f, C, D, A, B> ofHkt(__<__<__<__<__<µ, f>, C>, D>, A>, B> hkLift) {
      return (Lift3<f, C, D, A, B>) hkLift;
    }

    enum µ {}
  }
  
  private static class Lower<f, X, Y> implements __2<Lower.µ, X, Y> {
    final Lower2<f, ?, ?, X, Y> lower2;
    
    Lower(Lower2<f, ?, ?, X, Y> lower2) {
      this.lower2 = lower2;
    }
    
    static <f, X, Y> Lower<f, X, Y> ofHkt(__<__<Lower.µ, X>, Y> hkLower) {
      return (Lower<f, X, Y>)hkLower;
    }
    
    enum µ {}
  }

  private static class Lower2<f, A, B, X, Y> {
    final Leibniz<A,B> leib;
    final Leibniz<__<f,A>,X> leibX;
    final Leibniz<__<f,B>,Y> leibY;
    Lower2(Leibniz<A,B> leib, Leibniz<__<f,A>,X> leibX, Leibniz<__<f,B>,Y> leibY) {
      this.leib = leib;
      this.leibX = leibX;
      this.leibY = leibY;
    }
  }
    
}