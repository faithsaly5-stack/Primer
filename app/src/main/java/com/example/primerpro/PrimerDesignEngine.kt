package com.example.primerpro

import kotlin.math.abs

object PrimerDesignEngine {

    data class Primer(
        val sequence: String,
        val tm: Double,
        val gcPercent: Double,
        val startIndex: Int,
        val length: Int
    )

    data class PrimerPair(
        val forward: Primer,
        val reverse: Primer,
        val productSize: Int
    )

    fun designPrimers(targetSequence: String): PrimerPair? {
        val validSequence = targetSequence.replace(Regex("[^ATCGatcg]"), "").uppercase()
        if (validSequence.length < 50) return null // Sequence too short

        val validForwards = mutableListOf<Primer>()
        val validReversesByStart = mutableMapOf<Int, MutableList<Primer>>()

        // Heuristic: Limit search to the first few thousands bps and skip every other base to speed up significantly
        val maxSearchLimit = minOf(validSequence.length, 3000)

        // Find candidate forward primers
        for (i in 0..(maxSearchLimit - 18) step 2) {
            for (len in 18..22) {
                if (i + len > maxSearchLimit) break
                val forwardSeq = validSequence.substring(i, i + len)
                if (isValidPrimer(forwardSeq)) {
                    validForwards.add(Primer(forwardSeq, calculateTm(forwardSeq), calculateGC(forwardSeq), i, len))
                    if (validForwards.size >= 100) break // Keep maximum 100 good candidates
                }
            }
            if (validForwards.size >= 100) break
        }

        if (validForwards.isEmpty()) return null

        // Find candidate reverse primers only in the relevant region (50 to 1000 bp downstream of forwards)
        val minRevSearch = minOf(validForwards.first().startIndex + 50, validSequence.length)
        val maxRevSearch = minOf(validForwards.last().startIndex + 1000, validSequence.length)

        for (i in minRevSearch..(maxRevSearch - 18) step 2) {
            for (len in 18..22) {
                if (i + len > validSequence.length) break
                val targetRegion = validSequence.substring(i, i + len)
                val reverseSeq = reverseComplement(targetRegion)
                if (isValidPrimer(reverseSeq)) {
                    validReversesByStart.getOrPut(i) { mutableListOf() }.add(Primer(reverseSeq, calculateTm(reverseSeq), calculateGC(reverseSeq), i, len))
                }
            }
        }

        // Find the best pair
        var bestPair: PrimerPair? = null
        var bestScore = Double.MAX_VALUE

        for (forward in validForwards) {
            val minStart = forward.startIndex + 50
            val maxStart = minOf(forward.startIndex + 1000, validSequence.length)
            
            for (revStart in minStart..maxStart) {
                val reverses = validReversesByStart[revStart]
                if (reverses != null) {
                    for (reverse in reverses) {
                        val productSize = (reverse.startIndex + reverse.length) - forward.startIndex
                        if (productSize in 50..1000 && reverse.startIndex > (forward.startIndex + forward.length)) {
                            val tmDiff = abs(forward.tm - reverse.tm)
                            if (tmDiff <= 5.0 && !formsPrimerDimer(forward.sequence, reverse.sequence)) {
                                // Score based on tmDiff and optimal tm (around 60)
                                val score = tmDiff + abs(forward.tm - 60.0) + abs(reverse.tm - 60.0)
                                if (score < bestScore) {
                                    bestScore = score
                                    bestPair = PrimerPair(forward, reverse, productSize)
                                }
                            }
                        }
                    }
                }
            }
            
            // Early exit if an excellent pair is found to speed up calculation significantly
            if (bestScore < 1.0) {
                break
            }
        }

        return bestPair
    }

    private fun isValidPrimer(sequence: String): Boolean {
        val gc = calculateGC(sequence)
        if (gc < 40.0 || gc > 60.0) return false

        val tm = calculateTm(sequence)
        if (tm < 50.0 || tm > 65.0) return false

        if (!hasValidGCClamp(sequence)) return false
        if (hasPolyRun(sequence)) return false // Check poly run before hairpin as it's faster
        if (hasHairpin(sequence)) return false

        return true
    }

    private fun hasPolyRun(sequence: String): Boolean {
        // Fast imperative check instead of Regex
        var currentRun = 1
        for (i in 1 until sequence.length) {
            if (sequence[i] == sequence[i - 1]) {
                currentRun++
                if (currentRun >= 4) return true
            } else {
                currentRun = 1
            }
        }
        return false
    }

    private fun calculateGC(sequence: String): Double {
        val gcCount = sequence.count { it == 'G' || it == 'C' }
        return (gcCount.toDouble() / sequence.length) * 100.0
    }

    fun calculateTm(sequence: String): Double {
        val wA = sequence.count { it == 'A' }
        val xT = sequence.count { it == 'T' }
        val yG = sequence.count { it == 'G' }
        val zC = sequence.count { it == 'C' }
        
        // Simpler Wallace rule for < 14, but we only have 18-24. 
        // Using standard salt-adjusted formula: 64.9 + 41 * (G+C - 16.4)/(A+T+G+C)
        return 64.9 + 41.0 * (yG + zC - 16.4) / (wA + xT + yG + zC)
    }

    private fun hasValidGCClamp(sequence: String): Boolean {
        // Ensures the very last base is a G or C to anchor the polymerase
        if (sequence.last() == 'A' || sequence.last() == 'T') return false
        
        // Counts G/Cs in the last 5 bases. Ideal is 1, 2, or 3.
        val lastFive = sequence.takeLast(5)
        val gcCount = lastFive.count { it == 'G' || it == 'C' }
        
        return gcCount in 1..3
    }

    private fun hasHairpin(sequence: String): Boolean {
        val minStemLength = 5
        val minLoopLength = 3 // DNA backbone requires space to physically bend

        // Ensure we have enough sequence left for a stem, a loop, and the complementary stem
        for (i in 0..(sequence.length - (minStemLength * 2 + minLoopLength))) {
            val stem = sequence.substring(i, i + minStemLength)
            // Start looking for the complement AFTER the required loop space
            val rest = sequence.substring(i + minStemLength + minLoopLength)
            
            if (rest.contains(reverseComplement(stem))) {
                return true
            }
        }
        return false
    }

    private fun formsPrimerDimer(forward: String, reverse: String): Boolean {
        val minMatchLength = 5
        for (i in 0..(forward.length - minMatchLength)) {
            val substr = forward.substring(i, i + minMatchLength)
            if (reverse.contains(reverseComplement(substr))) {
                return true
            }
        }

        // 3' cross-dimers: strictly check if the 3' ends (last 4-5 bases) are complementary
        for (overlap in 4..5) {
            val f3 = forward.takeLast(overlap)
            val r3 = reverse.takeLast(overlap)
            if (f3 == reverseComplement(r3)) {
                return true
            }
        }

        return false
    }

    private fun reverseComplement(sequence: String): String {
        return sequence.reversed().map {
            when (it) {
                'A' -> 'T'
                'T' -> 'A'
                'G' -> 'C'
                'C' -> 'G'
                else -> it
            }
        }.joinToString("")
    }
}
