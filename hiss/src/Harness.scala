package hiss

import org.http4s.HttpRoutes

import cats.Id
import cats.effect.Concurrent
import cats.effect.IO
import cats.effect.Resource
import cats.effect.Sync
import smithy4s.Service
import smithy4s.http4s.SimpleRestJsonBuilder

object Harness {

  def suspend[Alg[_[_, _, _, _, _]], F[_]: Sync](
      base: Alg[smithy4s.kinds.Kind1[Id]#toKind5]
  )(using
      svc: Service[Alg]
  ): svc.Impl[F] =
    svc.impl(new svc.FunctorEndpointCompiler[F] {
      val baseF = svc.toPolyFunction(base)

      def apply[I, E, O, SI, SO](op: svc.Endpoint[I, E, O, SI, SO]): I => F[O] =
        in => Sync[F].delay(baseF(op.wrap(in)))
    })

  def routes[F[_]: Concurrent: Sync, Alg[_[_, _, _, _, _]]](
      base: Alg[smithy4s.kinds.Kind1[Id]#toKind5]
  )(using
      svc: Service[Alg]
  ): Resource[F, HttpRoutes[F]] =
    SimpleRestJsonBuilder.routes(suspend[Alg, F](base)).resource

  def routesIO[Alg[_[_, _, _, _, _]]](
      base: Alg[smithy4s.kinds.Kind1[Id]#toKind5]
  )(using
      svc: Service[Alg]
  ): Resource[IO, HttpRoutes[IO]] =
    routes[IO, Alg](base)

}
