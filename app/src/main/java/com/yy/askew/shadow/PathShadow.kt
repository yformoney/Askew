import android.graphics.BlurMaskFilter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.pathShadow(
    path: Density.(Size) -> Path,                     // 自定义的不规则路径
    offsetX: Dp = 0.dp,             // X轴阴影偏移（正值向右）
    offsetY: Dp = 0.dp,             // Y轴阴影偏移（正值向下）
    blurRadius: Dp = 8.dp,          // 模糊半径
    spread: Dp = 0.dp,              // 阴影扩展（正值扩大范围）
    color: Color = Color.Black.copy(alpha = 0.3f) // 阴影颜色（需透明度）
) = this.drawWithCache {
    onDrawBehind {
        // 转换参数为像素单位
        val offsetXPx = offsetX.toPx()
        val offsetYPx = offsetY.toPx()
        val blurRadiusPx = blurRadius.toPx()
        val spreadPx = spread.toPx()

        // 创建阴影专用Paint（启用模糊滤镜）
        val shadowPaint = Paint().apply {
            asFrameworkPaint().apply {
                maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
            }
            this.color = color
        }

        // 绘制阴影（通过底层Canvas）
        val originPath = path(size)
        drawIntoCanvas { canvas ->
            // 应用偏移和扩展
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.translate(offsetXPx, offsetYPx) // 偏移

            val bounds = originPath.getBounds()
            val centerX = bounds.left + bounds.width / 2
            val centerY = bounds.top + bounds.height / 2
            // 扩展路径：通过缩放Path实现阴影扩大
            val shadowPath = Path().apply {
                addPath(originPath) // 复制原始路径

                // 创建变换矩阵
                val matrix = Matrix().apply {
                    translate(-centerX, -centerY)               // 移动到原点
                    scale(                                       // 计算缩放因子
                        x = 1f + 2 * spreadPx / bounds.width,
                        y = 1f + 2 * spreadPx / bounds.height
                    )
                    translate(centerX, centerY)                 // 移回中心点
                }

                transform(matrix) // 应用矩阵变换
            }

            // 绘制阴影路径
            canvas.nativeCanvas.drawPath(shadowPath.asAndroidPath(), shadowPaint.asFrameworkPaint())
            canvas.nativeCanvas.restore()
        }

        // 绘制原始路径（覆盖在阴影上层）
        drawPath(path = originPath, color = Color.White) // 实际内容由调用方定义
    }
}

@Preview
@Composable
fun TopArcRoundedRectDemo() {
    Box(Modifier.size(400.dp)) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .pathShadow({ size ->// 参数定义
                    val rectWidth = size.width
                    val rectHeight = size.height
                    val cornerRadius = 16.dp.toPx()    // 基础圆角半径
                    val arcRadius = 16.dp.toPx() / 2      // 半圆凹陷深度

                    // 计算顶部半圆参数
                    val archStartX = rectWidth / 2 - arcRadius           // 半圆起点X坐标

                    val path = Path().apply {
                        // 1. 从左上角圆弧起点开始
                        moveTo(cornerRadius, 0f)

                        // 2. 绘制左侧到半圆起点的直线
                        lineTo(archStartX - arcRadius, 0f) // 移动到半圆左侧起点

                        // 3. 绘制顶部凹陷半圆（从180°到0°逆时针）[2](@ref)
                        arcTo(
                            rect = Rect(
                                left = archStartX - arcRadius,
                                top = -arcRadius,
                                right = archStartX + arcRadius,
                                bottom = arcRadius
                            ),
                            startAngleDegrees = 180f,     // 从左侧开始（180°方向）
                            sweepAngleDegrees = -180f,    // 逆时针扫180°形成凹陷
                            forceMoveTo = false           // 连接上一点[3](@ref)
                        )

                        // 4. 继续绘制右侧直线到右上角
                        lineTo(rectWidth - cornerRadius, 0f)

                        // 5. 绘制右上角圆弧（270°→0°）[1](@ref)
                        arcTo(
                            rect = Rect(
                                left = rectWidth - 2 * cornerRadius,
                                top = 0f,
                                right = rectWidth,
                                bottom = 2 * cornerRadius
                            ),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        // 6. 绘制右侧直线
                        lineTo(rectWidth, rectHeight - cornerRadius)

                        // 7. 绘制右下角圆弧
                        arcTo(
                            rect = Rect(
                                left = rectWidth - 2 * cornerRadius,
                                top = rectHeight - 2 * cornerRadius,
                                right = rectWidth,
                                bottom = rectHeight
                            ),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        // 8. 绘制底部直线
                        lineTo(cornerRadius, rectHeight)

                        arcTo(
                            rect = Rect(
                                left = archStartX - arcRadius,
                                top = rectHeight - arcRadius,
                                right = archStartX + arcRadius,
                                bottom = rectHeight + arcRadius
                            ),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = -180f,
                            forceMoveTo = false
                        )

                        // 9. 绘制左下角圆弧
                        arcTo(
                            rect = Rect(
                                left = 0f,
                                top = rectHeight - 2 * cornerRadius,
                                right = 2 * cornerRadius,
                                bottom = rectHeight
                            ),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        // 10. 绘制左侧直线回起点
                        lineTo(0f, cornerRadius)

                        // 11. 绘制左上角圆弧
                        arcTo(
                            rect = Rect(
                                left = 0f,
                                top = 0f,
                                right = 2 * cornerRadius,
                                bottom = 2 * cornerRadius
                            ),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )

                        close()
                    }
                    return@pathShadow path
                }, offsetY = 5.dp, color = Color.Red)
        )
    }
}