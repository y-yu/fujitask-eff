package domain.entity

case class User(id: UserId, name: String)

case class UserId(value: Long)
