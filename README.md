# Feign Help

针对 `Spring-Feign` 开发环境下，编译生成二方包下 `Feign` 的 `FallbackFactory`。

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.moyada/feign-help/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.moyada/feign-help)
![License MIT](https://img.shields.io/badge/MIT-342e38?style=flat-square&label=License)

## 安装

添加 `feign-help` maven 依赖到项目 **模块**中 *pom.xml* :

```
<dependency>
    <groupId>io.github.moyada</groupId>
    <artifactId>feign-help</artifactId>
    <version>1.0.2-RELEASE</version>
</dependency>
```

## 使用

#### 引用 Feign 接口

```
public interface UserApi {

    @RequestMapping(value = "user/get", method = RequestMethod.GET)
    Result<UserDTO> getUser(@RequestParam("userId") Long userId);

    @RequestMapping(value = "user/create", method = RequestMethod.POST)
    Result<Boolean> createUser(@RequestBody UserDTO data);

    @RequestMapping(value = "user/delete", method = RequestMethod.DELETE)
    Result<Boolean> deleteUser(@RequestBody UserRequest data);
}
```

#### 使用方构建 Feign Client

只需简单使用注解标记，经过编译过的class文件将会自动添加对应的 `Fallback`, `FallbackFactory`

```
@FallbackFactoryBuild
@FeignClient(name = "user")
public interface UserRemote extends UserApi {
}

```

#### 经过编译后的 UserRemote.class 文件

```
@FeignClient(
    name = "user",
    fallbackFactory = UserRemote.FallbackFactory.class
)
public interface UserRemote extends UserApi {
    @Component
    public static class FallbackFactory implements feign.hystrix.FallbackFactory {
        public FallbackFactory() {
        }

        public UserRemote create(Throwable arg0) {
            return new UserRemote.Fallback();
        }
    }

    public static class Fallback implements UserRemote {
        public Fallback() {
        }

        public Result<UserDTO> getUser(@RequestParam("userId") Long userId) {
            return null;
        }

        public Result<Boolean> createUser(@RequestBody UserDTO data) {
            return null;
        }

        public Result<Boolean> deleteUser(@RequestBody UserRequest data) {
            return null;
        }
    }
}
```

#### 指定方法返回值

可自定义构造 Fallback 中方法的返回值，支持基本类型、构造方法、静态方法，参数将会自动匹配出合适的方法。

1. 基本类型
```
@FeignReturn(target = boolean.class, params = ["ture"])
@FallbackFactoryBuild
@FeignClient(name = "user")
public interface UserRemote extends UserApi {
}

...

public interface UserRemote extends UserApi {

    public Boolean deleteUser(@RequestBody UserRequest data) {
         return true
    }
}

```

2. 构造函数
```
@FeignReturn(target = Result.class, staticMethod = "error", params = ["500", "msg"])
@FallbackFactoryBuild
@FeignClient(name = "user")
public interface UserRemote extends UserApi {
}

...

public interface UserRemote extends UserApi {

    public Result<Boolean> deleteUser(@RequestBody UserRequest data) {
         return new Result.error();
    }
}

```

3. 静态方法

```
@FeignReturn(target = Result.class, staticMethod = "error", params = ["500", "msg"])
@FallbackFactoryBuild
@FeignClient(name = "user")
public interface UserRemote extends UserApi {
}

...

public interface UserRemote extends UserApi {

    public Result<Boolean> deleteUser(@RequestBody UserRequest data) {
         return Result.error(500, "msg");
    }
}

```

## Contributors

| [![moyada](https://github.com/moyada.png?size=120)](https://github.com/r4phab) | [moyada](https://github.com/moyada) |
|:------------------------------------------------------------------------------:|--------------|
