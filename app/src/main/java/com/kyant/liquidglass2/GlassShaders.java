/*
   Copyright 2025 Kyant

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.kyant.liquidglass2;

/**
 * AGSL（Android Graphics Shading Language）着色器源码常量。
 *
 * <p>本文件逐字翻译自 kyant0 / AndroidLiquidGlass 的
 * {@code backdrop/src/commonMain/kotlin/com/kyant/backdrop/internal/Shaders.kt}，
 * 只在两处合并：
 * <ul>
 *   <li>去掉 Kotlin 字符串模板 {@code $RoundedRectSDF}，改为 Java 字符串拼接；</li>
 *   <li>折射与色散两套着色器在 Kotlin 侧是用一个 Boolean 切换的两个常量，这里保持两个常量不变，
 *       由 {@link GlassRenderer} 按 {@link GlassStyle#hasDispersion()} 选择。</li>
 * </ul>
 *
 * <p>所有着色器都需要 API 33（Android 13）以上的 {@link android.graphics.RuntimeShader}。
 */
public final class GlassShaders {

    private GlassShaders() {
    }

    /**
     * 圆角矩形的距离场（SDF）工具函数。三个函数都是纯数学，无副作用。
     *
     * <ul>
     *   <li>{@code radiusAt} —— 按象限取对应角的半径，radii = (左上, 右上, 右下, 左下)；</li>
     *   <li>{@code sdRoundedRect} —— 有符号距离，形状内部为负、外部为正；</li>
     *   <li>{@code gradSdRoundedRect} —— 解析梯度，即朝外的单位法线。比数值差分更稳、更快。</li>
     * </ul>
     */
    public static final String ROUNDED_RECT_SDF = ""
            + "float radiusAt(float2 coord, float4 radii) {\n"
            + "    if (coord.x >= 0.0) {\n"
            + "        if (coord.y <= 0.0) return radii.y;\n"
            + "        else return radii.z;\n"
            + "    } else {\n"
            + "        if (coord.y <= 0.0) return radii.x;\n"
            + "        else return radii.w;\n"
            + "    }\n"
            + "}\n"
            + "float sdRoundedRect(float2 coord, float2 halfSize, float radius) {\n"
            + "    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));\n"
            + "    float outside = length(max(cornerCoord, 0.0)) - radius;\n"
            + "    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);\n"
            + "    return outside + inside;\n"
            + "}\n"
            + "float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {\n"
            + "    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));\n"
            + "    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {\n"
            + "        return sign(coord) * normalize(max(cornerCoord, 0.0));\n"
            + "    } else {\n"
            + "        float gradX = step(cornerCoord.y, cornerCoord.x);\n"
            + "        return sign(coord) * float2(gradX, 1.0 - gradX);\n"
            + "    }\n"
            + "}\n";

    /**
     * 折射（不含色散）。对应设计面板的「折射 / 深度」两个滑杆。
     *
     * <p>核心是 {@code circleMap}：入参在轮廓线上为 1、在折射带内沿为 0，
     * 而 {@code circleMap} 在 x→1 时导数趋于无穷，所以偏移量在轮廓线附近陡峭爆发、
     * 往内部迅速衰减——这正是凸透镜边缘的数学特征。
     *
     * <p>注意 {@code -sd >= refractionHeight} 的提前 return：玻璃中心没有视差，
     * 直接返回原像素，一个 sqrt 都不用算。
     */
    public static final String REFRACTION = ""
            + "uniform shader content;\n"
            + "uniform float2 size;\n"
            + "uniform float2 offset;\n"
            + "uniform float4 cornerRadii;\n"
            + "uniform float refractionHeight;\n"
            + "uniform float refractionAmount;\n"
            + "uniform float depthEffect;\n"
            + ROUNDED_RECT_SDF
            + "float circleMap(float x) {\n"
            + "    return 1.0 - sqrt(1.0 - x * x);\n"
            + "}\n"
            + "half4 main(float2 coord) {\n"
            + "    float2 halfSize = size * 0.5;\n"
            + "    float2 centeredCoord = (coord + offset) - halfSize;\n"
            + "    float radius = radiusAt(coord, cornerRadii);\n"
            + "    float sd = sdRoundedRect(centeredCoord, halfSize, radius);\n"
            + "    if (-sd >= refractionHeight) {\n"
            + "        return content.eval(coord);\n"
            + "    }\n"
            + "    sd = min(sd, 0.0);\n"
            + "    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;\n"
            + "    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));\n"
            + "    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius)"
            + " + depthEffect * normalize(centeredCoord));\n"
            + "    float2 refractedCoord = coord + d * grad;\n"
            + "    return content.eval(refractedCoord);\n"
            + "}\n";

    /**
     * 折射 + 色散。对应设计面板的「色散」滑杆（布尔开关即可，开与不开是两倍性能差）。
     *
     * <p>色散不是简单的 RGB 三通道偏移，而是采样 7 个光谱位置：
     * 红 / 橙 / 黄 / 绿 / 青 / 蓝 / 紫。三通道权重各自归一化
     * （红 = 1/3.5×3 + 1/7 ≈ 1.0，绿 = 1/7 + 1/3.5×3 ≈ 1.0，蓝 = 1/3×3 = 1.0，
     * alpha = 1/7×7 = 1），所以整体不会串色或变暗，只是通道之间错位。
     *
     * <p>{@code dispersionIntensity} 乘了 {@code (x/hx) × (y/hy)}：这个因子在四个角上最大、
     * 在十字中轴线上恰好为 0，也就是色散只在角落显现——模拟真实玻璃「四角视角变化最大」，
     * 比全局均等偏移自然得多。去掉这个因子，色散会均匀糊成一圈脏边。
     */
    public static final String REFRACTION_WITH_DISPERSION = ""
            + "uniform shader content;\n"
            + "uniform float2 size;\n"
            + "uniform float2 offset;\n"
            + "uniform float4 cornerRadii;\n"
            + "uniform float refractionHeight;\n"
            + "uniform float refractionAmount;\n"
            + "uniform float depthEffect;\n"
            + "uniform float chromaticAberration;\n"
            + ROUNDED_RECT_SDF
            + "float circleMap(float x) {\n"
            + "    return 1.0 - sqrt(1.0 - x * x);\n"
            + "}\n"
            + "half4 main(float2 coord) {\n"
            + "    float2 halfSize = size * 0.5;\n"
            + "    float2 centeredCoord = (coord + offset) - halfSize;\n"
            + "    float radius = radiusAt(coord, cornerRadii);\n"
            + "    float sd = sdRoundedRect(centeredCoord, halfSize, radius);\n"
            + "    if (-sd >= refractionHeight) {\n"
            + "        return content.eval(coord);\n"
            + "    }\n"
            + "    sd = min(sd, 0.0);\n"
            + "    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;\n"
            + "    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));\n"
            + "    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius)"
            + " + depthEffect * normalize(centeredCoord));\n"
            + "    float2 refractedCoord = coord + d * grad;\n"
            + "    float dispersionIntensity = chromaticAberration"
            + " * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));\n"
            + "    float2 dispersedCoord = d * grad * dispersionIntensity;\n"
            + "    half4 color = half4(0.0);\n"
            + "    half4 red = content.eval(refractedCoord + dispersedCoord);\n"
            + "    color.r += red.r / 3.5;\n"
            + "    color.a += red.a / 7.0;\n"
            + "    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));\n"
            + "    color.r += orange.r / 3.5;\n"
            + "    color.g += orange.g / 7.0;\n"
            + "    color.a += orange.a / 7.0;\n"
            + "    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));\n"
            + "    color.r += yellow.r / 3.5;\n"
            + "    color.g += yellow.g / 3.5;\n"
            + "    color.a += yellow.a / 7.0;\n"
            + "    half4 green = content.eval(refractedCoord);\n"
            + "    color.g += green.g / 3.5;\n"
            + "    color.a += green.a / 7.0;\n"
            + "    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));\n"
            + "    color.g += cyan.g / 3.5;\n"
            + "    color.b += cyan.b / 3.0;\n"
            + "    color.a += cyan.a / 7.0;\n"
            + "    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));\n"
            + "    color.b += blue.b / 3.0;\n"
            + "    color.a += blue.a / 7.0;\n"
            + "    half4 purple = content.eval(refractedCoord - dispersedCoord);\n"
            + "    color.r += purple.r / 7.0;\n"
            + "    color.b += purple.b / 3.0;\n"
            + "    color.a += purple.a / 7.0;\n"
            + "    return color;\n"
            + "}\n";

    /**
     * 默认高光：两侧对称的镜面光，对应设计面板的「高光角度 / 强度」。
     *
     * <p>{@code pow(abs(dot(grad, normal)), falloff)} 里的 {@code abs} 是关键——
     * 它让轮廓的两侧同时亮起；{@code falloff} 控制锐度。
     * 这个着色器输出的是「透明度分布」，被当作描边 {@link android.graphics.Paint} 的
     * shader 去画一整圈轮廓，不需要法线贴图、不需要额外几何。
     *
     * <p>{@code layout(color)} 修饰符要求用
     * {@link android.graphics.RuntimeShader#setColorUniform(String, int)} 赋值。
     */
    public static final String HIGHLIGHT_DEFAULT = ""
            + "uniform float2 size;\n"
            + "uniform float4 cornerRadii;\n"
            + "layout(color) uniform half4 color;\n"
            + "uniform float angle;\n"
            + "uniform float falloff;\n"
            + ROUNDED_RECT_SDF
            + "half4 main(float2 coord) {\n"
            + "    float2 halfSize = size * 0.5;\n"
            + "    float2 centeredCoord = coord - halfSize;\n"
            + "    float radius = radiusAt(coord, cornerRadii);\n"
            + "    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));\n"
            + "    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);\n"
            + "    float2 normal = float2(cos(angle), sin(angle));\n"
            + "    float d = dot(grad, normal);\n"
            + "    float intensity = pow(abs(d), falloff);\n"
            + "    return color * intensity;\n"
            + "}\n";

    /**
     * 环境光高光：只亮一侧（{@code step(0.0, d)} 把背光面置零），
     * 用来做「整块玻璃被环境照亮」的底光，没有方向性强度差异。
     */
    public static final String HIGHLIGHT_AMBIENT = ""
            + "uniform float2 size;\n"
            + "uniform float4 cornerRadii;\n"
            + "uniform float angle;\n"
            + "uniform float falloff;\n"
            + ROUNDED_RECT_SDF
            + "half4 main(float2 coord) {\n"
            + "    float2 halfSize = size * 0.5;\n"
            + "    float2 centeredCoord = coord - halfSize;\n"
            + "    float radius = radiusAt(coord, cornerRadii);\n"
            + "    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));\n"
            + "    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);\n"
            + "    float2 normal = float2(cos(angle), sin(angle));\n"
            + "    float d = dot(grad, normal);\n"
            + "    float intensity = pow(abs(d), falloff);\n"
            + "    float t = step(0.0, d);\n"
            + "    return half4(t, t, t, 1.0) * intensity;\n"
            + "}\n";

    /**
     * 跟手高光：以手指位置为圆心的径向光斑，配合加色混合叠在玻璃上。
     * 对应 AndroidLiquidGlass 仓库 {@code app/.../utils/InteractiveHighlight.kt}。
     *
     * <p>这个效果用 {@link android.graphics.RadialGradient} 也能做，
     * 但用同一个 RuntimeShader 通道更统一，且能直接在 WGSL/AGSL 里继续加花样。
     */
    public static final String PRESS_RADIAL = ""
            + "uniform float2 size;\n"
            + "layout(color) uniform half4 color;\n"
            + "uniform float radius;\n"
            + "uniform float2 position;\n"
            + "half4 main(float2 coord) {\n"
            + "    float dist = distance(coord, position);\n"
            + "    float intensity = smoothstep(radius, radius * 0.5, dist);\n"
            + "    return color * intensity;\n"
            + "}\n";
}
