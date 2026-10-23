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

import android.graphics.Canvas;
import android.graphics.Path;

/**
 * 玻璃表面自定义绘制回调。对应上游 {@code drawBackdrop(onDrawSurface = ...)}。
 *
 * <h3>为什么需要它</h3>
 * 设计稿里几乎每个玻璃容器都不是单色填充，而是叠了好几层：
 *
 * <pre>
 * Continue 按钮 ：白色 100%  →  线性渐变 100%  →  绿色线性渐变 100%
 * 键盘按键      ：白色 15%   →  线性渐变 80%    →  线性渐变 15%
 * view chip     ：白色 15%
 * </pre>
 *
 * {@link GlassStyle#surfaceColor()} 只能表达第一层。要做渐变、要叠多层，就用这个回调。
 *
 * <h3>绘制顺序</h3>
 * 玻璃的四层是：折射 → 表面填充 → 按压光 → 边缘高光。
 * 这个回调插在「表面填充」之后、「按压光」之前。所以：
 * <ul>
 *   <li>在这里画不透明渐变，会把折射<b>盖住</b>（绿色 Continue 按钮就是这种情况，
 *       视觉上只剩边缘高光起作用——这正是设计稿的样子）；</li>
 *   <li>想保留折射，就给渐变带一点透明度。</li>
 * </ul>
 *
 * <h3>怎么画</h3>
 * <pre>{@code
 * style = GlassStyle.builder()
 *         .capsule()
 *         .onDrawSurface(new GlassSurface() {
 *             public void drawSurface(Canvas canvas, Path shapePath, float w, float h) {
 *                 Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
 *                 p.setShader(new LinearGradient(0, 0, 0, h, 0xFF3FD968, 0xFF00C853,
 *                         Shader.TileMode.CLAMP));
 *                 canvas.drawPath(shapePath, p);   // 直接画形状路径，边缘自然抗锯齿
 *             }
 *         })
 *         .build();
 * }</pre>
 *
 * 简单情形直接用 {@link GlassSurfaces} 里的工厂方法，不用自己写匿名类。
 *
 * <p><b>不要在这个回调里用 {@code canvas.clipPath(shapePath)}。</b>
 * 硬件加速画布的 clipPath 没有抗锯齿，会把上面几层已经做好的柔和边缘切硬。
 * 需要限制范围就画形状路径本身——它天然就是玻璃的轮廓。
 */
public interface GlassSurface {

    /**
     * @param canvas    画布，已经处于玻璃的局部坐标系（{@code (0,0)} 是玻璃左上角）
     * @param shape
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     * Path 玻璃的形状路径（圆角矩形 / 胶囊 / 圆），直接用它可以拿到抗锯齿边缘
     * @param width     玻璃宽度（px）
     * @param height 玻璃高度（px）
     */
    void drawSurface(Canvas canvas, Path shapePath, float width, float height);
}
