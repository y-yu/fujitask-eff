Fujitask Eff
========================

[![Build Status](https://travis-ci.org/y-yu/fujitask-eff.svg?branch=master)](https://travis-ci.org/y-yu/fujitask-eff)

## Abstract

This is a abstract data structure for transactions of databases by Extensible Effects. It can determine automatically which minimal transaction will be needed for executing SQLs.

For example, 

```scala
case class User(id: Long, name: String)
// create table `user` (
//   `id` bigint not null auto_increment,
//   `name` varchar(64) not null
// )

val logger: Logger = LoggerFactory.getLogger(Main.getClass)

val eff1 = for {
  user1 <- userRepository.read(1L)
  _     <- userRepository.create("test")
  user2 <- userRepository.read(1L)
} yield {
  logger.info(s"user1 is $user1")
  logger.info(s"user2 is $user2")
}
Fujitask.run(eff1)
```

then the console logs are below.

```
02:08:31.610 [run-main-1] INFO  repository.impl.jdbc.package$ - ReadWriteRunner begin --------->
02:08:31.672 [scala-execution-context-global-82] INFO  Main$ - user1 is None
02:08:31.672 [scala-execution-context-global-82] INFO  Main$ - user2 is Some(User(1,test))
02:08:31.674 [scala-execution-context-global-82] INFO  repository.impl.jdbc.package$ - <--------- ReadWriteRunner end
```

After if we would use only the `read` function like that:

```scala
val eff2 = for {
  user3 <- userRepository.read(1L)
} yield {
  logger.info(s"user3 is $user3")
}
Fujitask.run(eff2)
```  

we can show that a read-only transaction would be used.

```
02:08:31.675 [scala-execution-context-global-84] INFO  repository.impl.jdbc.package$ - ReadRunner begin --------->
02:08:31.676 [scala-execution-context-global-104] INFO  Main$ - user3 is Some(User(1,test))
02:08:31.677 [scala-execution-context-global-104] INFO  repository.impl.jdbc.package$ - <--------- ReadRunner end
``` 

In addition, we can use any other monads together in the same `for`-`yield`.

```scala
val eff3 = for {
  name <- Reader.ask[String]
  user <- userRepository.create(name)
  user4 <- userRepository.read(user.id)
} yield {
  logger.info(s"user4 is $user4")
}
Fujitask.run(Reader.run("piyo")(eff3))
```

```
02:20:15.892 [scala-execution-context-global-134] INFO  repository.impl.jdbc.package$ - ReadWriteRunner begin --------->
02:20:15.893 [scala-execution-context-global-134] INFO  Main$ - user4 is Some(User(2,piyo))
02:20:15.893 [scala-execution-context-global-133] INFO  repository.impl.jdbc.package$ - <--------- ReadWriteRunner end
```

If you would like to get more information about this, please contact me. I will be happy to help you if it would be possible.

## How to use

You can try it by the following command.

```console
./sbt run
```

And it can be tested as follows.


```console
./sbt test
```

## Acknowledgments

I would like to thank [@halcat0x15a](https://github.com/halcat0x15a) for many advices to implement.

## References

- [Extensible Effectsでトランザクションモナド“Fujitask”を作る](https://qiita.com/yyu/items/fbd6edc00abb6395dabb)
    - This is my article for this implementation written in Japanese
- [ドワンゴ秘伝のトランザクションモナドを解説！](https://qiita.com/pab_tech/items/86e4c31d052c678f6fa6)
- [kits-eff](https://github.com/halcat0x15a/kits-eff)
- [進捗大陸05](https://booth.pm/ja/items/1309694)
- [Freer Monads, More Extensible Effects](http://okmij.org/ftp/Haskell/extensible/more.pdf)
