package zzb.rest

package directives

import shapeless._
import spray.routing.{ PathMatcher, ImplicitPathMatcherConstruction, PathMatchers }
import spray.routing.PathMatchers.{ PathEnd, Slash }
import spray.routing.PathMatcher.{ Unmatched, Matched }

trait PathDirectives extends PathMatchers with ImplicitPathMatcherConstruction {
  import BasicDirectives._
  import RouteDirectives._
  import PathMatcher._

  /**
   * Tries to consume a leading slash from the unmatched path of the [[zzb.rest.RestReqContext]]
   * before applying the given matcher. The matcher has to match the remaining path completely
   * or leave only a single trailing slash.
   * If matched the value extracted by the PathMatcher is extracted on the directive level.
   */
  def path[L <: HList](pm: PathMatcher[L]): Directive[L] = pathPrefix(pm ~ PathEnd)

  /**
   * Tries to consume a leading slash from the unmatched path of the [[zzb.rest.RestReqContext]]
   * before applying the given matcher. The matcher has to match a prefix of the remaining path.
   * If matched the value extracted by the PathMatcher is extracted on the directive level.
   */
  def pathPrefix[L <: HList](pm: PathMatcher[L]): Directive[L] = rawPathPrefix(Slash ~ pm)

  /**
   * Applies the given matcher directly to the unmatched path of the [[zzb.rest.RestReqContext]]
   * (i.e. without implicitly consuming a leading slash).
   * The matcher has to match a prefix of the remaining path.
   * If matched the value extracted by the PathMatcher is extracted on the directive level.
   */
  def rawPathPrefix[L <: HList](pm: PathMatcher[L]): Directive[L] =
    extract(ctx ⇒ pm(ctx.unmatchedPath)).flatMap {
      case Matched(rest, values) ⇒ hprovide(values) & mapRequestContext(_.copy(unmatchedPath = rest))
      case Unmatched             ⇒ reject
    }

  /**
   * Checks whether the unmatchedPath of the [[zzb.rest.RestReqContext]] has a prefix matched by the
   * given PathMatcher. In analogy to the `pathPrefix` directive a leading slash is implied.
   */
  def pathPrefixTest[L <: HList](pm: PathMatcher[L]): Directive[L] = rawPathPrefixTest(Slash ~ pm)

  /**
   * Checks whether the unmatchedPath of the [[zzb.rest.RestReqContext]] has a prefix matched by the
   * given PathMatcher. However, as opposed to the `pathPrefix` directive the matched path is not
   * actually "consumed".
   */
  def rawPathPrefixTest[L <: HList](pm: PathMatcher[L]): Directive[L] =
    extract(ctx ⇒ pm(ctx.unmatchedPath)).flatMap {
      case Matched(_, values) ⇒ hprovide(values)
      case Unmatched          ⇒ reject
    }

  /**
   * Rejects the request if the unmatchedPath of the [[zzb.rest.RestReqContext]] does not have a suffix
   * matched the given PathMatcher. If matched the value extracted by the PathMatcher is extracted
   * and the matched parts of the path are consumed.
   * Note that, if the given PathMatcher is a compound one consisting of several concatenated sub-matchers,
   * the order of the sub-matchers in the concatenation has to be reversed!
   */
  def pathSuffix[L <: HList](pm: PathMatcher[L]): Directive[L] =
    extract(ctx ⇒ pm(ctx.unmatchedPath.reverse)).flatMap {
      case Matched(rest, values) ⇒ hprovide(values) & mapRequestContext(_.copy(unmatchedPath = rest.reverse))
      case Unmatched             ⇒ reject
    }

  /**
   * Checks whether the unmatchedPath of the [[zzb.rest.RestReqContext]] has a suffix matched by the
   * given PathMatcher. However, as opposed to the pathSuffix directive the matched path is not
   * actually "consumed".
   */
  def pathSuffixTest[L <: HList](pm: PathMatcher[L]): Directive[L] =
    extract(ctx ⇒ pm(ctx.unmatchedPath.reverse)).flatMap {
      case Matched(_, values) ⇒ hprovide(values)
      case Unmatched          ⇒ reject
    }

  /**
   * Rejects the request if the unmatchedPath of the [[zzb.rest.RestReqContext]] is non-empty,
   * or said differently: only passes on the request to its inner route if the request path
   * has been matched completely.
   */
  def pathEnd: Directive0 = rawPathPrefix(PathEnd)

  /**
   * Only passes on the request to its inner route if the request path has been matched
   * completely or only consists of exactly one remaining slash.
   */
  def pathEndOrSingleSlash: Directive0 = rawPathPrefix(Slash.? ~ PathEnd)

  /**
   * Only passes on the request to its inner route if the request path
   * consists of exactly one remaining slash.
   */
  def pathSingleSlash: Directive0 = pathPrefix(PathEnd)
}

object PathDirectives extends PathDirectives