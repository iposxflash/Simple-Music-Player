package com.simplemobiletools.musicplayer.helpers

import android.content.Context
import com.simplemobiletools.commons.extensions.addBit
import com.simplemobiletools.commons.extensions.getParentPath
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.musicplayer.extensions.*
import com.simplemobiletools.musicplayer.inlines.indexOfFirstOrNull
import com.simplemobiletools.musicplayer.models.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AudioHelper(private val context: Context) {

    private val config = context.config

    fun insertTracks(tracks: List<Track>) {
        context.tracksDAO.insertAll(tracks)
    }

    fun getTrack(mediaStoreId: Long): Track? {
        // Karena kita bypass database, kita cari manual di daftar assets
        return getAllTracks().firstOrNull { it.mediaStoreId == mediaStoreId }
    }

    // --- PERBAIKAN UTAMA: MEMBACA DARI ASSETS ---
    fun getAllTracks(): ArrayList<Track> {
        val tracks = ArrayList<Track>()
        val assetManager = context.assets
        
        try {
            // Membaca file langsung dari folder assets
            val files = assetManager.list("") ?: return tracks
            var idCounter = 1L
            
            for (filename in files) {
                // Filter hanya file audio yang didukung
                if (filename.endsWith(".mp3", true) || 
                    filename.endsWith(".flac", true) || 
                    filename.endsWith(".m4a", true)) {
                    
                    val track = Track(
                        id = idCounter, 
                        mediaStoreId = idCounter, 
                        title = filename.substringBeforeLast("."), 
                        artist = "Internal Music", 
                        path = "asset:///$filename", // Path khusus untuk ExoPlayer
                        duration = 0, 
                        album = "App Bundle", 
                        artistId = 1L, 
                        albumId = 1L, 
                        trackId = idCounter.toInt(), 
                        folderName = "Assets"
                    )
                    tracks.add(track)
                    idCounter++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks.sortSafely(config.trackSorting)
        return tracks
    }

    fun getAllFolders(): ArrayList<Folder> {
        // Karena semua ada di assets, kita kembalikan satu folder virtual saja
        val folders = ArrayList<Folder>()
        folders.add(Folder("Assets", getAllTracks().size, "internal/assets"))
        return folders
    }

    fun getFolderTracks(folder: String): ArrayList<Track> {
        return getAllTracks()
    }

    fun updateTrackInfo(newPath: String, artist: String, title: String, oldPath: String) {
        // Non-aktifkan karena file assets bersifat read-only
    }

    fun deleteTrack(mediaStoreId: Long) {
        // Non-aktifkan karena file assets tidak bisa dihapus dari dalam APK
    }

    fun deleteTracks(tracks: List<Track>) {}

    fun insertArtists(artists: List<Artist>) {}

    fun getAllArtists(): ArrayList<Artist> {
        val artist = Artist(1L, "Internal Artist", getAllTracks().size, getAllTracks().size)
        return arrayListOf(artist)
    }

    fun getArtistAlbums(artistId: Long): ArrayList<Album> {
        val album = Album(1L, "App Bundle", "Internal Artist", getAllTracks().size, 2024, "", 1L)
        return arrayListOf(album)
    }

    fun getArtistAlbums(artists: List<Artist>): ArrayList<Album> {
        return getArtistAlbums(1L)
    }

    fun getArtistTracks(artistId: Long): ArrayList<Track> {
        return getAllTracks()
    }

    fun getArtistTracks(artists: List<Artist>): ArrayList<Track> {
        return getAllTracks()
    }

    fun deleteArtist(id: Long) {}

    fun deleteArtists(artists: List<Artist>) {}

    fun insertAlbums(albums: List<Album>) {}

    fun getAlbum(albumId: Long): Album? {
        return Album(1L, "App Bundle", "Internal Artist", getAllTracks().size, 2024, "", 1L)
    }

    fun getAllAlbums(): ArrayList<Album> {
        return getArtistAlbums(1L)
    }

    fun getAlbumTracks(albumId: Long): ArrayList<Track> {
        return getAllTracks()
    }

    fun getAlbumTracks(albums: List<Album>): ArrayList<Track> {
        return getAllTracks()
    }

    fun getAllPlaylists(): ArrayList<Playlist> {
        return ArrayList()
    }

    fun getAllGenres(): ArrayList<Genre> {
        return ArrayList()
    }

    fun getQueuedTracks(queueItems: List<QueueItem> = context.queueDAO.getAll()): ArrayList<Track> {
        val allTracks = getAllTracks().associateBy { it.mediaStoreId }

        val tracks = queueItems.mapNotNull { queueItem ->
            val track = allTracks[queueItem.trackId]
            if (track != null) {
                if (queueItem.isCurrent) {
                    track.flags = track.flags.addBit(FLAG_IS_CURRENT)
                }
                track
            } else {
                null
            }
        }

        return if (tracks.isEmpty()) getAllTracks() else tracks as ArrayList<Track>
    }

    fun getQueuedTracksLazily(callback: (tracks: List<Track>, startIndex: Int, startPositionMs: Long) -> Unit) {
        ensureBackgroundThread {
            val allTracks = getAllTracks()
            if (allTracks.isEmpty()) {
                callback(emptyList(), 0, 0)
                return@ensureBackgroundThread
            }

            val currentItem = context.queueDAO.getCurrent()
            val startPositionMs = currentItem?.lastPosition?.seconds?.inWholeMilliseconds ?: 0
            
            // Cari index lagu saat ini
            val currentIndex = if (currentItem != null) {
                allTracks.indexOfFirstOrNull { it.mediaStoreId == currentItem.trackId } ?: 0
            } else 0

            callback(allTracks, currentIndex, startPositionMs)
        }
    }

    fun initQueue(): ArrayList<Track> {
        val tracks = getAllTracks()
        val queueItems = tracks.mapIndexed { index, mediaItem ->
            QueueItem(trackId = mediaItem.mediaStoreId, trackOrder = index, isCurrent = index == 0, lastPosition = 0)
        }

        resetQueue(queueItems)
        return tracks
    }

    fun resetQueue(items: List<QueueItem>, currentTrackId: Long? = null, startPosition: Long? = null) {
        context.queueDAO.deleteAllItems()
        context.queueDAO.insertAll(items)
        if (currentTrackId != null && startPosition != null) {
            val startPositionSeconds = startPosition.milliseconds.inWholeSeconds.toInt()
            context.queueDAO.saveCurrentTrackProgress(currentTrackId, startPositionSeconds)
        } else if (currentTrackId != null) {
            context.queueDAO.saveCurrentTrack(currentTrackId)
        }
    }
}

private fun Collection<Track>.applyProperFilenames(showFilename: Int): ArrayList<Track> {
    return this as ArrayList<Track>
}
