package robocop.database

import io.getquill._
import robocop.models.{Configuration, Metrics, Pings}

class Robobase {

  lazy val ctx = new SqliteJdbcContext(SnakeCase, "ctx")

  import ctx._

  object metrics {
    def apply: Map[Int, Metrics] = ctx.run(query[Metrics].map(metric => (metric.id, metric))).toMap

    def +=(metric: Metrics): Int = ctx.run(query[Metrics].insert(lift(metric)).returning(_.id))
  }

  object pings {

    def apply: Map[Int, Pings] = ctx.run(query[Pings].map(ping => (ping.id, ping))).toMap

    def +=(ping: Pings): Int = ctx.run(query[Pings].insert(lift(ping)).returning(_.id))
  }

  object config {

    def all: Map[Long, Configuration] = ctx.run(query[Configuration]).map(x => (x.guild, x)).toMap

    def apply(id: Long): Option[Configuration] = ctx.run(query[Configuration].filter(_.guild == lift(id))).headOption

    def +=(configuration: Configuration): Unit = {
      ctx.run(query[Configuration].insert(lift(configuration)))
    }

    def ==(configuration: Configuration): Unit = {
      ctx.run(query[Configuration].update(lift(configuration)))
    }
  }

  def sqlAction(sql: String): Unit = {
    ctx.executeAction(sql)
  }

}
