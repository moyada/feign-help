# Feign Help

针对 `Spring-Feign` 开发环境下，编译生成二方包下 `Feign` 的 `FallbackFactory`。

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.moyada/feign-help/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.moyada/feign-help)
![License MIT](https://img.shields.io/badge/MIT-342e38?style=flat-square&label=License)

## Installation

In your **module** *pom.xml* :

```
<dependency>
    <groupId>io.github.moyada</groupId>
    <artifactId>feign-help</artifactId>
    <version>1.0.0-REPLEASE</version>
</dependency>
```

## Usage

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

#### 构建 Feign Client

只需简单使用注解标记，经过编译过的class文件将会自动添加对应的 `FallbackFactory`

```
@FallbackFactoryBuild
@FeignClient(name = "user", fallbackFactory = UserRemote.FallbackFactory.class)
public interface UserRemote extends UserApi {
}

```

#### 经过编译后 UserRemote.class 文件

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

## Contributors

| [![moyada](https://github.com/moyada.png?size=120)](https://github.com/r4phab) | [moyada](https://github.com/moyada) |
|:------------------------------------------------------------------------------:|--------------|
