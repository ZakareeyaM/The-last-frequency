package com.zak.lastfreq.core

import kotlin.math.abs
import kotlin.math.sqrt

/** Collision and interaction queries for one scene. Positions are in design pixels (32 per tile). */
class World(val scene: Scene) {
    private val t = Content.TILE
    val widthPx: Float get() = scene.w * t
    val heightPx: Float get() = scene.h * t

    private fun tileSolid(tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= scene.w || ty >= scene.h) return true
        val c = scene.tiles[ty][tx]
        return c == '#' || c == '~' || c == 'T' || c == ' '
    }

    private fun boxBlocked(cx: Float, cy: Float, hw: Float, hh: Float): Boolean {
        val x0 = ((cx - hw) / t).toInt().let { if (cx - hw < 0f) -1 else it }
        val x1 = ((cx + hw) / t).toInt()
        val y0 = ((cy - hh) / t).toInt().let { if (cy - hh < 0f) -1 else it }
        val y1 = ((cy + hh) / t).toInt()
        for (ty in y0..y1) for (tx in x0..x1) if (tileSolid(tx, ty)) return true
        for (o in scene.objects) {
            if (!o.solid) continue
            val ox = o.x * t
            val oy = o.y * t
            if (cx + hw > ox && cx - hw < ox + o.w * t && cy + hh > oy && cy - hh < oy + o.h * t) return true
        }
        return false
    }

    fun blocked(x: Float, y: Float): Boolean = boxBlocked(x, y, 8f, 6f)

    /** Moves pos by (dx, dy) with axis-separated collision. */
    fun move(pos: FloatArray, dx: Float, dy: Float) {
        if (!blocked(pos[0] + dx, pos[1])) pos[0] += dx
        if (!blocked(pos[0], pos[1] + dy)) pos[1] += dy
    }

    /** Frees the player if a position loaded from a save happens to be inside something solid. */
    fun unstick(pos: FloatArray) {
        if (!blocked(pos[0], pos[1])) return
        pos[0] = scene.spawnX * t
        pos[1] = scene.spawnY * t
    }

    fun distanceTo(o: Obj, px: Float, py: Float): Float {
        val ox = o.x * t
        val oy = o.y * t
        val cx = px.coerceIn(ox, ox + o.w * t)
        val cy = py.coerceIn(oy, oy + o.h * t)
        val dx = px - cx
        val dy = py - cy
        return sqrt(dx * dx + dy * dy)
    }

    fun nearest(px: Float, py: Float, reach: Float = 30f): Obj? {
        var best: Obj? = null
        var bd = reach
        for (o in scene.objects) {
            if (o.interact == null) continue
            val d = distanceTo(o, px, py)
            if (d < bd) {
                bd = d
                best = o
            }
        }
        return best
    }

    fun lightSources(): List<Obj> = scene.objects.filter { it.light > 0f }

    fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float = sqrt((ax - bx) * (ax - bx) + (ay - by) * (ay - by)) + 0f * abs(ax)
}
