# TODO — Upsilon Roadmap

Items needed to support complex dynamic UIs beyond the current baseline.

## Performance

- [ ] **局部重建 / 子树标记** — `rebuild()` 当前触发整棵树重建。需要 Diff/子树 Dirty 标记或 Key 机制，让动态组件只重建自身子树。
- [ ] **渲染缓存** — 对不变子树做 Renderable 级缓存，避免每帧重建。

## Animation

- [ ] **属性动画** — 对位置、大小、透明度、旋转等属性做插值动画。
- [ ] **缓动函数** — ease-in/out、spring、bounce 等常用缓动。
- [ ] **过渡动画** — 页面切换动画、元素出现/消失过渡（fade、slide、scale）。

## Interaction

- [ ] **长按** — `onLongPress` 回调，含起始延迟与移动容差。
- [ ] **拖拽** — drag start/move/end 事件链，含拖拽数据载体。
- [ ] **双击** — 与单击区分，含双击时间窗口。
- [ ] **鼠标进入/离开** — `onHoverEnter` / `onHoverLeave`，目前仅有点击悬停判定。

## Focus & Keyboard

- [ ] **焦点链** — 组件树的 Tab 序遍历。
- [ ] **键盘快捷键** — 全局与组件级键绑定。
- [ ] **焦点指示器** — 键盘导航时可见焦点高亮。

## Overlay / Modal

- [ ] **对话框** — 模态弹窗，背景遮罩 + 阻塞下层交互。
- [ ] **下拉菜单** — 依附于触发控件的浮动菜单。
- [ ] **上下文菜单** — 右键弹出菜单。
- [ ] **Toast / 通知** — 短暂非模态提示。

## Layout

- [ ] **网格布局** — 类似 Flutter `GridView` 的等宽/等比例网格。
- [ ] **表格布局** — 类 `Table`，按行列对齐。
- [ ] **Wrap / Flow** — 自动换行的流式布局。

## State Management

- [ ] **StatefulWidget 等价物** — 内建状态持有与自动重建的便捷封装。
- [ ] **InheritedWidget 等价物** — 子树作用域状态传递，避免逐层传递参数。
- [ ] **不可变状态快照** — 与 Diff 机制配合，提供前后状态对比。

## Misc

- [ ] **主题切换** — 运行时切换主题并触发重建。
- [ ] **响应式断点** — 按窗口宽度切换布局策略。
- [ ] **国际化** — 文本键到翻译的映射管道。
