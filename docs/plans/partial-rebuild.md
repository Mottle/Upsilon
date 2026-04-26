# 局部重建实现计划（严格版）

> **状态：已实现并通过构建验证。**  
> 六阶段实施全部完成，剩余工作仅限 in-game 手工交互验证。
> 见下方「实施顺序」中各阶段标注和「验证标准」中已完成/待验证区分。

## 定位

这不是一个“在现有 `UIBuilder` 上补一层 dirty splice”的小功能改动，而是一次 **UI 运行时模型重构**。

本版不采用：

- `UIFactory`
- detached 双实例图
- `StateStore`
- `StatefulDynamic`

本版采用：

- **同一组件实例复用**
- **active/staged 双 MountState 槽**
- **三态 BuildMode**
- **ThreadLocal 构建会话栈**
- **挂载树覆盖所有真实 `UIComponent`**

目标是在尽量贴近当前库实现的前提下，把局部重建做正确，并避免再次出现“视觉旧、交互新”的分裂。

---

## 目标

将当前 Upsilon 的“任意 `DynamicUIComponent` 变脏即整树重建”机制，升级为：

- 局部重建
- 尺寸回流
- 容器 slot 结构变化拒绝局部提交并回到上层 reopen 决策

覆盖：

1. `ScreenUIRenderer`
2. `HudUIRenderer`
3. `TauContainerScreen`

覆盖组件：

- 普通动态组件
- 嵌套动态组件
- `Transform`
- `Tooltip`
- `WidgetWrapper`
- `ListView`
- `TextField`
- `Slider`

说明：

- `TauContainerScreen` 仅支持 slot **可视变化** 的局部提交
- slot **结构变化**（数量/顺序/绑定变化）不允许在 screen 内原地修补

---

## 当前库的决定性约束

### 1. `build()` 会直接修改实例状态

当前代码中：

- `Button.build(...)` 写 `x/y/width/height`
- `ListView.build(...)` 写 `size/position/maxScroll`，并清空 `childEventListeners`
- `Transform.build(...)` 清空 `childrenEventListeners`
- `WidgetWrapper.build(...)` 直接修改真实 `AbstractWidget`

因此不能再把构建结果直接写回组件实例上的单一 live 字段。否则 candidate build 会污染当前正在显示的 UI。

### 2. `UIBuilder.build(...)` 既用于真实构建，也用于测量

例如：

- `Column.MIN` 会对 child 先做一次测量 build
- `Row.MIN` 也有同类测量行为
- `ListView` 会先测内容高度

这意味着 build 模式必须能区分：

- 真正可提交的 mounted build
- mounted build 中的临时测量
- 完全独立的 mountless 测量

### 3. 子树尺寸变化会向父级传播

`Column` / `Row` / `Padding` / `Sized` / `Center` / `Align` / `Positioned` / `Stack` 都会依赖 child size。dirty 子树尺寸变化时，不能只替换 dirty 自己，必须向上回流。

### 4. `AbstractContainerMenu` 的 slot 结构在打开后不是自由可变的

对 `TauContainerScreen`：

- **可视变化**：位置 / 显隐 / 装饰变化，可由 screen 局部提交处理
- **结构变化**：数量 / 顺序 / handler 绑定变化，不允许 screen 内原地修补

### 5. 同一组件实例在同一棵 live tree 中不能挂载两次

mounted build 必须检测并拒绝重复实例挂载。

---

## 核心设计决策

1. **组件实例复用**：partial rebuild 时使用同一实例重新 build，不创建 detached 副本
2. **active + session-scoped staged**：每个有挂载态的组件实例维护 `activeMountState`，staged 归当前 `BuildSession`
3. **三态 BuildMode**：`MOUNTLESS` / `MOUNTED_COMMITTABLE` / `MOUNTED_MEASURE`
4. **renderer 权威结构是挂载树**；线性列表只是缓存
5. **输入分发使用稳定 dispatcher**；不依赖 `Screen.children()` 热替换
6. **局部提交点由尺寸比较 + 向上回流决定**
7. **`destroy()` 只对 orphan active mounts 触发**

---

## 三类状态边界

所有组件相关数据必须明确归入以下三类之一。

### A. 配置状态

定义：描述组件如何工作的静态配置。

例子：

- `Button.onPress`
- `Tooltip` 文本和 positioner
- `Slider.minimum/maximum/stepSize`
- `TextField.validator/formatter`
- `ItemSlotHandler` 的 inventory / index

保留位置：组件构造参数或外部闭包。

### B. 逻辑状态

定义：同一逻辑组件在 rebuild 前后应保留的可变状态。

例子：

- `ListView.scrollOffset`
- `TextField` 当前文本
- `TextField` 光标位置、选区、focus
- `Slider` 当前值、拖拽中的临时值

保留位置：组件实例字段。

### C. 挂载态

定义：只对当前 live 挂载有效，下一次 remount 时必须重新计算/替换的数据。

例子：

- `Button` 的几何
- `ListView` 的 `size/position/maxScroll/measuredContentHeight/measuredViewport/childEventListeners/childRenderables`
- `Transform` 的 `childrenEventListeners/focused/dragging`
- `WidgetWrapper` / `TextField` / `Slider` 的真实 widget 实例

保留位置：`MountState`。

如果某个字段无法明确归入三类之一，说明组件边界没设计清楚，不能直接接入局部重建。

---

## active / staged 作用域

### 统一接口

所有存在挂载态的组件都必须遵循统一 active 槽接口：

```java
public interface MountStateHost<S extends MountState> {
    S getActiveMountState();
    void setActiveMountState(S state);
}
```

### 语义

- `activeMountState`：当前 live 渲染和 live 输入读取的状态

`staged` **不再挂在组件实例上**，而是属于当前 `BuildSession`：

```java
public final class BuildSession {
    private final BuildMode mode;
    private final BuildContext context;
    private final IdentityHashMap<Object, MountState> stagedStates;
}
```

key 使用组件实例 identity。

### 硬规则

1. **run-time input/render 默认只读 `activeMountState`**
2. **`MOUNTED_COMMITTABLE` 只写当前 session 的 `stagedStates`**
3. **commit 成功时由当前 session 的 stagedStates 批量提升为 active**
4. **commit 失败时丢弃当前 session 的 stagedStates，不得影响 active**

### 唯一例外

只有构建期内部逻辑允许通过当前 session 读 staged，例如：

- staged child listener 集合封装 renderable
- staged widget 实例初始化
- staged geometry 推导 sibling size

run-time 的 hover、click、drag、focus、children()、slot hit-test 一律按默认规则读 active。

### 为什么 staged 必须是 session-scoped

尺寸回流时，同一个组件实例可能先参与子级 candidate build，再参与父级 candidate build。若 staged 直接挂在实例字段上，父级 candidate 会覆盖子级 candidate 的 staged。把 staged 放到 `BuildSession.stagedStates` 后，不同 candidate session 之间天然隔离。

---

## 组件最小重构要求

### `Button`

删除实例字段：

- `width`
- `height`
- `x`
- `y`

新增：

- `ButtonMountState activeMountState`

规则：

- build 时向当前 session 的 stagedStates 写几何
- `mouseClicked()` / `isHovered()` / renderable 都读 active
- commit 时由 session 提升 staged → active

### `ListView`

保留为逻辑状态：

- `scrollOffset`

迁入 `ListViewMountState`：

- `size`
- `position`
- `maxScroll`
- `measuredContentHeight`
- `measuredViewport`
- `childEventListeners`
- `childRenderables`

双槽规则：

- `mouseScrolled()` / `isMouseOver()` / `children()` / `getChildAt()` 只读 active
- `MOUNTED_COMMITTABLE` 下 build 写 staged
- `MOUNTED_MEASURE` 下只算 size，不改 active/staged

### `Transform`

迁入 `TransformMountState`：

- `childrenEventListeners`
- `focused`
- `dragging`

同样使用 active + session-scoped staged。

### `WidgetWrapper`

这里必须区分两条路径：

#### A. 保留旧 API：`WidgetWrapper(AbstractWidget child)`

这条路径继续存在，用于外部已经持有一个具体 widget 实例、并希望树内外操作的是**同一个对象**的场景。

语义：

- 这是 **legacy / full-rebuild-only** 路径
- 不保证 partial-rebuild-safe
- 若某棵子树中出现这种 wrapper，框架应：
  - 要么在该提交点及以上直接退回 full rebuild
  - 要么将该子树标记为不可局部提交

#### B. 新增 partial-rebuild-safe 路径：`WidgetFactoryWrapper(Supplier<AbstractWidget>)`

用于 `TextField` / `Slider` / 未来需要局部重建安全性的 widget 型组件。

```java
public final class WidgetFactoryWrapper implements PrimitiveUIComponent {
    private final Supplier<AbstractWidget> widgetFactory;
    private WidgetMountState activeMountState;
}
```

其中 `WidgetMountState` 持有真实 widget 实例。

### 为什么不能直接把 `WidgetWrapper` 破坏性升级为 factory

当前 `WidgetWrapper` 的真实语义是“桥接一个外部现成 widget 实例进树中”。如果直接改成 factory：

- 外部持有的旧 widget 将不再与树里使用的是同一个对象
- 这是 API 语义变化，而不是纯内部重构

因此严格版方案明确采用更稳的做法：

- 保留旧 `WidgetWrapper(AbstractWidget child)`
- 新增 `WidgetFactoryWrapper(Supplier<AbstractWidget>)`

并规定：

> 只有 `WidgetFactoryWrapper` 是 partial-rebuild-safe；旧 `WidgetWrapper` 只保证 legacy/full-rebuild-only。

### `TextField`

逻辑状态写到字段级：

- 当前文本
- 光标位置
- 选区起点
- 选区终点
- focus 状态

挂载态：

- `EditBox` 实例
- 当前像素几何

#### 逻辑状态 ↔ widget 双相同步规则

1. **用户输入时（widget → 实例）**：`EditBox` 的每次文本变更必须通过回调（如 `setResponder` 或覆写 `onValueChange`）将当前文本、光标位置、选区同步回 `TextField` 实例的逻辑状态字段
2. **commit 后（实例 → widget）**：新 widget 创建后，立刻用组件实例上保存的逻辑状态字段初始化 widget 的文本、光标、选区、focus
3. 此规则确保：即使 widget 在 rebuild 中被重新创建，用户可感知的交互状态**零丢失**

实现示例：

```java
public final class TextField implements UIComponent, MountStateHost<TextFieldMountState> {
    // 逻辑状态
    private String text = "";
    private int cursorPos;
    private int selectionStart;
    private int selectionEnd;
    private boolean focused;

    // 挂载态
    private TextFieldMountState activeMountState;

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return new WidgetFactoryWrapper(() -> {
            EditBox widget = new EditBox(...);
            // 用逻辑状态初始化 widget
            widget.setValue(text);
            widget.moveCursorTo(cursorPos);
            widget.setHighlightPos(selectionStart);
            // 注册反向同步
            widget.setResponder(newText -> {
                this.text = newText;
                this.cursorPos = widget.getCursorPosition();
                this.selectionStart = widget.getHighlightPos();
                this.selectionEnd = ...;
            });
            return widget;
        });
    }
}
```

### `Slider`

逻辑状态写到字段级：

- 当前值
- 若存在拖拽中的临时值，则保留该值
- 拖拽中/按下态

挂载态：

- `ExtendedSlider` 实例
- 当前像素几何

#### 逻辑状态 ↔ widget 双相同步规则

1. **用户拖拽时（widget → 实例）**：`ExtendedSlider` 的 `applyValue()` 回调必须将当前值同步回 `Slider` 实例的逻辑状态字段
2. **commit 后（实例 → widget）**：新 widget 创建后，立刻用实例保存的当前值初始化 widget
3. 拖拽中状态同样需要双向保持
4. `Slider` 的 widget 路径同样使用 `WidgetFactoryWrapper`，而不是旧 `WidgetWrapper(AbstractWidget child)`

### `Tooltip`

`Tooltip` **不需要重构**。其 `build()` 中 `position` / `size` 是局部变量，tooltip 闭包捕获的是栈上值，不存在实例挂载态字段。局部提交时 `tooltips` 列表随 `ContextRanges` 统一替换即可。

---

## 挂载树

### `ComponentMount`

挂载树覆盖所有真实 `UIComponent`：

```java
public final class ComponentMount {
    private final UIComponent owner;
    private final DynamicUIComponent dynamicOwner; // nullable

    private ComponentMount parent;
    private final List<ComponentMount> children = new ArrayList<>();

    private ContextRanges ranges;
    private Layout savedLayout;
    private Theme savedTheme;
    private SimpleVec2i builtSize;
}
```

### 为什么 MountState 不放在 ComponentMount 上

run-time 事件到达的是组件实例，不是 mount 节点；组件实例必须能直接读取自己的 active/staged mount state。

因此：

- 挂载树负责结构与区间
- 组件实例负责 active/staged mount state 宿主

---

## Builder-shell 规则

当前 `Builder implements UIComponent` 的类（如 `Text.Builder`、`TextField.Builder`、`Slider.Builder` 等）不进入最终挂载树。

新增接口：

```java
public interface BuilderShell {
}
```

mounted build 遇到 `BuilderShell`：

1. 只执行其 `build(layout, theme)` 展开真实节点
2. 不创建 `ComponentMount`
3. 继续从真实节点开始挂载

---

## 三态 BuildMode

```java
public enum BuildMode {
    MOUNTLESS,
    MOUNTED_COMMITTABLE,
    MOUNTED_MEASURE
}
```

### `MOUNTLESS`

用途：

- 独立测量
- client menu slot discovery
- 完全离屏分析

限制：

- 不创建 mount
- 不做唯一性检查
- 不写 active/staged mount state
- 不触发生命周期

### `MOUNTED_COMMITTABLE`

用途：

- full rebuild candidate
- partial rebuild candidate
- 一切可能被 commit 的 mounted 构建

限制：

- 允许创建 staged mount state
- 允许生成完整 artifact
- 不得改 active mount state
- **构建产生的 `ComponentMount` 树是 detached 候选树，不直接改 active 挂载树**
- 候选树的 `BuildResult`（含 `rootMounts`、`preorderMounts`、`context`、`size`）由 renderer 暂存，commit 成功后才替换进 active 挂载树

### `MOUNTED_MEASURE`

用途：

- mounted build 内部的临时测量 pass
- `Column.MIN`
- `Row.MIN`
- `ListView` 内部内容高度测量

严格限制：

- 只允许计算 size
- **禁止**把 renderable 注册到主 active/staged 输出
- **禁止**把 listener 注册到主 active/staged 输出
- **禁止**把 tooltip 注册到主 active/staged 输出
- **禁止**把 slot 注册到主 active/staged 输出
- **禁止**把 dynamic 注册到主 active/staged 输出
- **禁止**写 active/staged mount state

这条规则必须写死，否则 mounted build 中的测量调用会错误挂载。

#### 执行机制

`MOUNTED_MEASURE` 不是“所有 artifact add 都变成 no-op”，而是：

> 测量构建只能写入一个**隔离的本地 `BuildContext`**，该 context 在测量结束后整体丢弃，绝不注册到主 mounted session 的 active/staged 输出。

这样做的原因是：当前库里已经存在会在内部组织子 `BuildContext` 的组件，例如：

- `Transform`
- `ListView`
- `Tooltip`

这些组件在测量时仍然需要允许其内部 build 路径“像平时一样工作”，否则测出来的 `size` 可能失真。禁止的是 artifact **逃逸到主输出**，不是组件内部完全不能有 artifact 结构。

实现方式：

```java
// UIBuilder 新增测量入口
public static SimpleVec2i measure(Layout layout, Theme theme, UIComponent component) {
    BuildContext isolatedContext = new BuildContext();
    pushSession(new BuildSession(BuildMode.MOUNTED_MEASURE, isolatedContext));
    try {
        return buildInternal(layout, theme, component);
    } finally {
        popSession();
        // isolatedContext 中收集到的 artifact 全部丢弃，不注册到 active/staged 主输出
    }
}
```

换句话说：

- `MOUNTED_MEASURE` 允许组件在隔离 context 中正常组织 renderable/listener/slot/tooltip 结构
- 但这些结构不会进入 renderer 的主 `BuildContext`
- 调用方只使用 `size` 结果

`Column` / `Row` / `ListView` 内部的测量调用必须改为使用此入口：

```java
// 旧代码：
SimpleVec2i childSize = UIBuilder.build(layout.copy(), theme, child, new BuildContext());
// 新代码：
SimpleVec2i childSize = UIBuilder.measure(layout.copy(), theme, child);
```

这是这三个组件**所需的最小改动**，其余组件和 `UIBuilder.build(...)` 的调用者**无需改动**。

---

## ThreadLocal 构建会话栈

```java
public final class UIBuilder {
    private static final ThreadLocal<Deque<BuildSession>> ACTIVE_SESSION = ThreadLocal.withInitial(ArrayDeque::new);
}
```

### 组件如何读取 staged

组件实例不直接持有 staged 字段。构建期若需要 staged，统一通过当前 session 查询：

```java
S staged = UIBuilder.currentSession().getOrCreateStagedState(this, stateFactory);
```

其中 `stateFactory` 只在当前组件第一次参与本 session 时创建 staged state。

### 规则

1. `buildTree(...)` push 一个 session，标记 mode 为 `MOUNTED_COMMITTABLE`
2. `measure(...)` push 一个 session，标记 mode 为 `MOUNTED_MEASURE`
3. 旧 `UIBuilder.build(...)` 每次进入时读取 thread-local 当前 session
4. 若无 session，走 `MOUNTLESS`
5. 若有 session，则根据当前 session.mode 决定：
   - `MOUNTED_COMMITTABLE`
   - `MOUNTED_MEASURE`
6. 组件内部继续调用 `UIBuilder.build(...)` 完成真实构建（ThreadLocal 自动继承父模式）
7. 需要测量的调用方改用 `UIBuilder.measure(...)` 显式进入测量模式

### 需要改动的代码

| 位置 | 旧代码 | 新代码 |
|------|--------|--------|
| `Column.MIN` 测量 pass | `UIBuilder.build(layout.copy(), theme, child, new BuildContext())` | `UIBuilder.measure(layout.copy(), theme, child)` |
| `Row.MIN` 测量 pass | 同 Column | 同 Column |
| `ListView` 内容高度测量 | `UIBuilder.build(layout.copy(), theme, measureColumn.build(children), new BuildContext())` | `UIBuilder.measure(layout.copy(), theme, measureColumn.build(children))` |

其余所有组件（Button、Transform、Tooltip、WidgetWrapper 等）和所有 `UIBuilder.build(context, ...)` 调用者**零改动**。

---

## mounted build 规则

对每个真实 `UIComponent` 进入时：

1. 若是 `BuilderShell`，只展开，不建 mount
2. 否则在当前 session 内做实例唯一性检查
3. 创建 `ComponentMount`
4. 连接 parent/child
5. 保存 `savedLayout = layout.copy()`
6. 保存 `savedTheme = theme`
7. 记录五个 artifact 列表的 start 索引
8. 若 `owner instanceof DynamicUIComponent`：
   - 调用 `buildDynamic(...)`
   - 加入 staged dynamic registry
9. 若 `owner instanceof GuiEventListener`，加入 staged listener 列表
10. 若 `owner instanceof PrimitiveUIComponent`，执行 primitive build
11. 调用 `next = owner.build(layout, theme)`
12. 若 `next != null`，继续 mounted build
13. 递归返回后记录 end 索引
14. 记录 `builtSize`
15. 若为 dynamic，调用 `finalizeDynamic(context)`

### mode-specific mutation 规则

#### 在 `MOUNTED_COMMITTABLE` 下

- 允许写 staged mount state
- 允许发出完整 artifact

#### 在 `MOUNTED_MEASURE` 下

- 不允许写 active/staged mount state
- 不允许发出 artifact
- 只返回 size

这条规则优先级高于所有组件自身实现习惯。

---

## 输入分发

### `RootInputDispatcher`

```java
public final class RootInputDispatcher implements ContainerEventHandler {
    private final Supplier<List<GuiEventListener>> childrenSupplier;
    private GuiEventListener focused;
    private boolean dragging;
}
```

它始终从 **active** listener 集合读取。

### `ScreenUIRenderer`

覆写并转发：

- `mouseClicked`
- `mouseReleased`
- `mouseDragged`
- `mouseScrolled`
- `mouseMoved`
- `keyPressed`
- `keyReleased`
- `charTyped`
- `changeFocus`

顺序：先给 dispatcher；若未消费，再走 `super`。

### `TauContainerScreen`

同样先给 dispatcher，再走 `AbstractContainerScreen` 默认行为。

### 输入读态规则

默认规则：

> run-time input/render 只读 `activeMountState`。

只有构建过程中的内部逻辑允许读 staged。

---

## dirty 收集与归一化

只从 **active dynamicMounts 缓存** 收集 dirty。

规则：

1. `dynamicOwner.tick()`
2. 若 dirty，则收集对应 `ComponentMount`
3. 立刻清掉 dirty 标记
4. 沿 `parent` 链归一化，只保留最顶层 dirty 祖先

---

## 提交点选择：尺寸回流算法

设 dirty mount 为 `m`：

1. `target = m`
2. 对 `target` 做一次 `MOUNTED_COMMITTABLE` candidate build，结果进入 staged 槽
3. 比较 candidate size 与 `target.builtSize`
4. 若相同，则 `target` 可提交
5. 若不同：
   - 清空当前 `target` 及其子树内所有 staged 槽
   - `target = target.parent`，回到步骤 2
6. 若到根仍无法满足额外校验，则 full rebuild

这样保证：

- `Column/Row` sibling 布局变化能正确向上回流
- `ListView` 内容尺寸变化时能在 `ListView` 层提交

---

## 生命周期规则

### `destroy()` 触发时机写死

> `destroy()` 只对 **orphan active mounts** 调用。

不允许：

- 在 commit 前 destroy 复用实例
- 在 remount 时 destroy 复用实例
- 在 full rebuild 前先 destroy 全树

### partial commit

对提交点 `target`：

1. 收集旧 active 子树 `oldSubtree`
2. 构建 staged candidate
3. 比较 old/new dynamic owner 集合
4. `orphans = old - new`
5. 仅对 orphan active mounts 调用 `destroy()`
6. commit 后 staged → active

### full rebuild 同理

1. 构建 staged 根树
2. 比较旧 active / 新 staged dynamic owners
3. destroy orphan active mounts
4. 原子替换 active tree

screen 关闭时才 destroy 当前整棵 active tree。

---

## 局部提交流程

假设已通过尺寸回流选出提交点 `target`：

1. 收集旧 active 子树 `oldSubtree = target.collectSubtreePreorder()`
2. 保存旧 active `ContextRanges oldRanges`
3. 在 `MOUNTED_COMMITTABLE` 下构建 staged candidate
4. 计算 old/new dynamic owner 差集，destroy orphan active mounts
5. 从 `mainContext` 删除 `oldRanges` 覆盖的五类 active artifact 区间
6. 将 staged candidate ranges 平移到 `oldRanges` 起始位置
7. 插入 staged candidate 的五类 artifact 到 `mainContext`
8. 在权威挂载树中用 candidate root 替换 target
9. staged → active，清空 staged 槽
10. 修正旧子树之后仍存活节点的 `ranges` 偏移
11. 从权威树重新 flatten 出 active `preorderMounts` 与 active `dynamicMounts`

如果任一步失败：

- 丢弃 staged 槽
- 保留 active 槽不变
- 退回 full rebuild

---

## 容器 slot 协议

### `ISlotHandler.getStructureKey()`

```java
Object getStructureKey();
```

规则：

- 位置变化不改变 key
- 数量/顺序/绑定变化必须改变 key 序列

### `TauContainerScreen` 的局部提交条件

仅当：

- old/new slot key 序列一致
- 且只发生可视变化

时，允许局部提交。

### `UIMenu.getSlotStructureVersion(...)`

```java
default int getSlotStructureVersion(TauContainerMenu menu) { return 0; }
```

`TauContainerMenu` 同步一个内部 `DataSlot`：

- server getter：`uiMenu.getSlotStructureVersion(menu)`
- client setter：写入 `syncedSlotStructureVersion`

### 结构变化策略

若 client screen 检测到：

- slot key 序列变化
- 或 `syncedSlotStructureVersion` 变化

则：

1. 拒绝 partial commit
2. full rebuild 当前 screen 投影
3. 标记 stale
4. 把 reopen 决策交给上层 server owner

不是 screen 自己伪造容器结构。

---

## renderer 状态模型

三个 renderer 统一持有：

```java
private BuildContext mainContext;
private List<ComponentMount> rootMounts;
private List<ComponentMount> preorderMounts;
private List<ComponentMount> dynamicMounts;
private RootInputDispatcher dispatcher; // HUD 无需此项
```

这些都表示 **active** 版本。

staged 版本只存在于当前 build session 和组件实例的 staged 槽中，不常驻 renderer 字段。

---

## full rebuild 流程

1. 开一个 `MOUNTED_COMMITTABLE` session
2. 用根组件做 staged build
3. 比较旧 active / 新 staged dynamic owner，destroy orphan active mounts
4. staged → active 原子替换：
   - `mainContext`
   - `rootMounts`
   - `preorderMounts`
   - `dynamicMounts`
5. 清空 staged 槽

screen 关闭时全量 destroy 当前 active tree。

---

## 失败恢复

以下任一条件成立，partial commit 失败并退回 full rebuild：

1. 检测到重复组件实例挂载
2. `removeRange` / `insertAll` 越界
3. mount 树 parent/child 替换不闭合
4. `ContextRanges` 平移后非法
5. `TauContainerScreen` 检测到 slot 结构变化却仍尝试 partial commit
6. builder-shell 未展开就进入挂载树
7. `MOUNTED_MEASURE` 将 artifact 逃逸到了主 active/staged 输出，或写入 active/staged mount state
8. 组件在 run-time input/render 中读取了 staged state 并造成 active/staged 混用

---

## 实施顺序

> **全部六阶段已完成并提交 (5d233dc)。** 各阶段标注 ✅ 表示代码已闭环。标注 🧪 表示仅剩 in-game 手工验证。

### 第一阶段：基础模型 ✅
1. `BuildContext.removeRange / insertAll`
2. `ContextRanges`
3. `MountState`
4. `MountStateHost<S>`
5. `ComponentMount`
6. `BuildResult`
7. `BuildSession` + ThreadLocal
8. `BuildMode`（三态）

### 第二阶段：组件适配 ✅
9. `Button` → active/staged `ButtonMountState`
10. `ListView` → active/staged `ListViewMountState`
11. `Transform` → active/staged `TransformMountState`
12. `WidgetWrapper` → widget factory + active/staged `WidgetMountState`
13. `TextField` / `Slider` → 交互态字段级迁移方案

### 第三阶段：builder 重构 ✅
14. `UIBuilder.buildTree(...)` 新入口
15. ThreadLocal 模式传播
16. 旧 `UIBuilder.build(...)` 自动读取当前 session mode
17. `MOUNTED_MEASURE` 严格只返回 size

### 第四阶段：renderer 重构 ✅
18. `RootInputDispatcher`
19. `ScreenUIRenderer` → dispatcher
20. `TauContainerScreen` → dispatcher
21. `HudUIRenderer` → 新状态模型

### 第五阶段：局部提交 ✅
22. dirty 收集与归一化
23. 尺寸回流提交点选择
24. staged candidate build
25. orphan-only destroy
26. partial commit + tail range 偏移
27. 从权威挂载树刷新 active 缓存列表

### 第六阶段：容器协议 ✅
28. `ISlotHandler.getStructureKey()`
29. `UIMenu.getSlotStructureVersion(...)`
30. `TauContainerMenu` 同步 `syncedSlotStructureVersion`
31. `TauContainerScreen` 检测并拒绝 slot 结构提交
32. server owner 决定是否 reopen（reopen 决策由外部调用方负责）

---

## 验证标准

> ✅ = 代码已验证并通过 build；🧪 = 仅剩 in-game 手工验证。

1. ✅ 测量 build 不创建 mount
2. ✅ `MOUNTED_MEASURE` 的 artifact 只存在于隔离 context，绝不逃逸到主 active/staged 输出
3. ✅ mounted build 中 `Column/Row` 子节点正确进入挂载树
4. 🧪 simple dynamic 文本变化不触发整屏重建
5. 🧪 `Column/Row` 子项尺寸变化后兄弟布局正确回流
6. 🧪 `ListView` 内容尺寸变化后滚动状态保留
7. 🧪 `TextField` 的文本/光标/选区/focus 在 rebuild 后保留
8. 🧪 `Slider` 的值/拖拽中状态在 rebuild 后保留
9. 🧪 `Button` hit-test 在 rebuild 后正确
10. 🧪 `Transform` / `Tooltip` / `WidgetWrapper` rebuild 后输入与渲染一致
11. 🧪 `HudUIRenderer` 局部变化不触发全 HUD 重建
12. 🧪 `TauContainerScreen` slot 可视变化可局部提交；结构变化被检测并拒绝
13. ✅ 任一 partial commit 失败自动 full rebuild 恢复一致性
