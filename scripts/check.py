#!/usr/bin/env python3
"""CarBack 架构静态一致性自检。

检查项：
1. 模块结构：settings.gradle.kts 与 17 个模块（app/opensdk:api/opensdk:impl + core x5 + feature x9）一致；
2. feature 三件套：每个 feature 均有 api/impl/demo 及各自 build.gradle.kts；
3. impl 依赖守卫：任何 feature impl 的 build.gradle.kts 不得引用其他 feature 的 impl/demo；
4. 路由唯一性：全部 @Route("...") 路径不得冲突；
5. convention 插件：build-logic 中恰好 7 个 carback-* 插件。

用法：python3 scripts/check.py  （全部通过退出码 0）
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ERRORS = []
CORE_MODULES = ["model", "ui", "router", "router-compiler", "debug-scaffold"]
FEATURES = ["home", "mine", "search"]
VARIANTS = ["api", "impl", "demo"]


def fail(msg: str) -> None:
    ERRORS.append(msg)


def check_settings() -> None:
    settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
    if 'include(":app")' not in settings:
        fail("settings.gradle.kts 缺少 :app")
    if 'include(":opensdk:api")' not in settings or 'include(":opensdk:impl")' not in settings:
        fail("settings.gradle.kts 缺少 :opensdk:api / :opensdk:impl")
    if 'listOf("model", "ui", "router", "router-compiler", "debug-scaffold")' not in settings:
        fail("settings.gradle.kts 缺少 core 五模块 include 循环")
    if 'listOf("home", "mine", "search")' not in settings or 'listOf("api", "impl", "demo")' not in settings:
        fail("settings.gradle.kts 缺少 feature 三件套 include 循环")


def check_feature_triplets() -> None:
    for feature in FEATURES:
        for variant in VARIANTS:
            base = ROOT / "feature" / feature / variant
            if not base.is_dir():
                fail(f"缺少模块目录 feature/{feature}/{variant}")
                continue
            if not (base / "build.gradle.kts").exists():
                fail(f"feature/{feature}/{variant} 缺少 build.gradle.kts")
            manifest = base / "src" / "main" / "AndroidManifest.xml"
            if variant in ("api", "impl") and not manifest.exists():
                fail(f"feature/{feature}/{variant} 缺少 AndroidManifest.xml")


def check_impl_guard() -> None:
    pattern = re.compile(r'project\(":feature:(\w+):(impl|demo)"\)')
    for feature in FEATURES:
        build_file = ROOT / "feature" / feature / "impl" / "build.gradle.kts"
        if not build_file.exists():
            continue
        for match in pattern.finditer(build_file.read_text(encoding="utf-8")):
            other, kind = match.groups()
            fail(f"守卫违规：feature/{feature}/impl 依赖了 :feature:{other}:{kind}"
                 f"（impl 之间禁止互相依赖，请改依赖对方 :api）")


def check_route_uniqueness() -> None:
    routes: dict[str, set[str]] = {}
    pattern = re.compile(r'@Route\("([^"]+)"\)')
    for source in (ROOT / "feature").rglob("*.kt"):
        if "build" in source.parts:
            continue
        for match in pattern.finditer(source.read_text(encoding="utf-8")):
            routes.setdefault(match.group(1), set()).add(
                str(source.relative_to(ROOT)))
    for path, owners in routes.items():
        if len(owners) > 1:
            fail(f"路由路径冲突：\"{path}\" 被 {sorted(owners)} 同时声明")


def check_convention_plugins() -> None:
    plugins = sorted((ROOT / "build-logic" / "src" / "main" / "kotlin").glob("*.gradle.kts"))
    carback_plugins = [p.name for p in plugins if p.name.startswith("carback-")]
    expected = {
        "carback-android-application.gradle.kts",
        "carback-android-library.gradle.kts",
        "carback-android-hilt.gradle.kts",
        "carback-feature-api.gradle.kts",
        "carback-feature-impl.gradle.kts",
        "carback-feature-demo.gradle.kts",
        "carback-kotlin-jvm.gradle.kts",
    }
    missing = expected - set(carback_plugins)
    if missing:
        fail(f"build-logic 缺少 convention 插件：{sorted(missing)}")
    if not (ROOT / "build-logic" / "settings.gradle.kts").exists():
        fail("build-logic 缺少 settings.gradle.kts（组合构建）")


def main() -> int:
    check_settings()
    check_feature_triplets()
    check_impl_guard()
    check_route_uniqueness()
    check_convention_plugins()

    if ERRORS:
        print("check.py FAILED:")
        for error in ERRORS:
            print(f"  - {error}")
        return 1
    modules = 3 + len(CORE_MODULES) + len(FEATURES) * len(VARIANTS)
    print(f"check.py PASSED（{modules} 个模块：app/opensdk:api/opensdk:impl + core x{len(CORE_MODULES)}"
          f" + feature 三件套 x{len(FEATURES)}）")
    print("  - feature 三件套结构完整")
    print("  - impl 无跨 feature 依赖")
    print("  - 路由路径无冲突")
    print("  - 7 个 convention 插件齐备")
    return 0


if __name__ == "__main__":
    sys.exit(main())
