# 无尽背包（Endless Inventory）

**Endless Inventory** 是一个为 Minecraft 设计的模组，提供了一个可扩展且容量无限的物品管理系统。
它引入了一个自定义 GUI，使物品浏览、排序和筛选更加便捷。
本模组非常适合 RPG 类型的玩法，或管理大量物品的技术型玩家。

## 版本

v 1.0.0（正式发布） - 对应 Minecraft 1.21.1
目前处于测试阶段，预计将逐步添加更多功能。

## 特性

* 使用指定物品（EndInv Accessor，物品 ID：`endless_inventory:test_endinv`）或按下快捷键 `I` 打开无尽背包。
* 支持分页显示，包含分类过滤器、高级排序与搜索功能。
* 使用 `config.toml` 文件自定义 EndInv 行为以满足不同需求：

    * 客户端配置：

        * 贴图模式
        * 是否将 EndInv 附加在菜单界面
        * EndInv 主菜单 / 附加界面的行列数
    * 服务端配置：

        * 玩家可插入的最大尺寸与单项最大尺寸
        * 自动拾取模式（该功能未来可能迁移至其他模组）

## 菜单操作

在附加了 EndInv 页面的 GUI 中：

* GUI 中按 `I`：打开 EndlessInventory 主界面
* 菜单中 `Ctrl + 左键`：快速将物品从菜单移动至 EndInv
* 页面区内按 `A`：添加物品到书签页
* 在书签页面区域按 `A`：将物品从书签中移除

## 指令说明

根指令：`/endinv`

* `backup`：备份 `endless_inventories.dat` 文件
* `new`：创建新的无尽背包

    * `public` / `restricted` / `private`：设置新背包的默认访问权限，默认为 `public`
* `ofIndex`：

    * 无参数：获取玩家当前所用背包在 `levelEndInvs` 中的索引
    * `<index>`：根据索引获取对应背包

        * `open`：打开背包，任何人都可执行，但只有拥有访问权限的玩家或 OP 能真正打开
        * `setDefault`：设置当前背包为玩家的默认背包
        * `setOwner`：将背包的所有者设置为执行者
        * `addWhiteList`：将执行者添加到该背包的访问白名单
        * `removeWhiteList`：将执行者从访问白名单中移除
        * `setAccessibility <public/restricted/private>`：设置该背包的访问权限
        * `remove`：从 `levelEndInvs` 中移除当前背包，操作前会自动创建备份文件

            * `<forceRemove>: boolean`：强制移除背包，即使备份失败

## 安装说明

1. 确保已安装适用于您 Minecraft 版本的 NeoForge。
2. 从 Releases 页面下载 Endless Inventory 的最新版本。
3. 将 `.jar` 文件放入 Minecraft 的 `mods` 文件夹中。
4. 启动游戏并享受全新的背包系统！

## 配置文件说明

本模组支持客户端与服务端的双端配置：

* `config/endless_inventory-server.toml`：控制服务端行为与背包模式
* `config/endless_inventory-client.toml`：控制界面布局、排序偏好与 GUI 显示选项

## 开发者指南

### 前置条件

* Java 21 或更高版本

### 开发环境

* 可通过 Gradle 构建脚本导入 IntelliJ IDEA 或 Eclipse 进行开发
