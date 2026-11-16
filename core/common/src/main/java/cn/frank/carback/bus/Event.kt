package cn.frank.carback.bus

/**
 * 跨 feature 事件公共标记接口。
 *
 * feature 的 api 模块可定义自己的事件数据类实现 [Event]，
 * 通过 [EventBus] 发送/接收。
 *
 * @author shangmingchao
 */
interface Event
