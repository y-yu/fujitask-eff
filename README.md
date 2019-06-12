Fujitask Eff
========================

[![Build Status](https://travis-ci.org/y-yu/fujitask-eff.svg?branch=master)](https://travis-ci.org/y-yu/fujitask-eff)

## Abstract

This is a abstract data structure for transactions of databases by Extensible Effects. 

```scala
val eff = for {
  user1 <- userRepository.read(1L)
  _     <- userRepository.create("test")
  user2 <- userRepository.read(1L)
  _     <- userRepository.create("test2")
} yield user2.get

Fujitask.run(eff) // User(1, test)
```

And we can use this with any other monads in the same `for`-`yield`.

```scala
val eff = for {
  n <- Reader.ask[String]
  user <- userRepository.create(n)
} yield user

Fujitask.run(Reader.run("test")(eff)) // User(1, test)
``` 

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

I would like to thank @halcat0x15a for many advices to implement.

## References

- [Extensible Effectsでトランザクションモナド“Fujitask”を作る](https://qiita.com/yyu/items/fbd6edc00abb6395dabb)
- [ドワンゴ秘伝のトランザクションモナドを解説！](https://qiita.com/pab_tech/items/86e4c31d052c678f6fa6)
- [kits-eff](https://github.com/halcat0x15a/kits-eff)
- [進捗大陸05](https://booth.pm/ja/items/1309694)
- [Freer Monads, More Extensible Effects](http://okmij.org/ftp/Haskell/extensible/more.pdf)
