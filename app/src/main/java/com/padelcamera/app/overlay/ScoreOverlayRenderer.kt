package com.padelcamera.app.overlay

import android.graphics.*

class ScoreOverlayRenderer(private val width: Int, private val height: Int) {

    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = height * 0.042f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = height * 0.09f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    fun render(player1Name: String, player2Name: String, score1: Int, score2: Int): Bitmap {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val pad = width * 0.018f
        val barHeight = height * 0.18f
        val cornerRadius = 12f

        // Background pill
        canvas.drawRoundRect(pad, pad, width - pad, pad + barHeight, cornerRadius, cornerRadius, bgPaint)

        val midX = width / 2f
        val nameY = pad + barHeight * 0.40f
        val scoreY = pad + barHeight * 0.88f

        // Player 1 — left half
        canvas.drawText(
            player1Name.take(18),
            pad * 2.5f,
            nameY,
            namePaint
        )
        canvas.drawText(
            score1.toString(),
            pad * 2.5f,
            scoreY,
            scorePaint
        )

        // Player 2 — right half
        canvas.drawText(
            player2Name.take(18),
            midX + pad * 2.5f,
            nameY,
            namePaint
        )
        canvas.drawText(
            score2.toString(),
            midX + pad * 2.5f,
            scoreY,
            scorePaint
        )

        // Centre divider line
        canvas.drawLine(midX, pad * 2f, midX, pad + barHeight - pad * 2f, dividerPaint)

        return bitmap
    }
}
