/**
 * Service for managing video encryption keys
 * Handles generation, storage, and retrieval of encryption keys
 */

class EncryptionKeyService {
  constructor() {
    this.storageKey = 'spillway_encryption_keys'
  }

  /**
   * Generate a secure encryption key
   * @returns {string} Base64 encoded encryption key
   */
  generateKey() {
    // Generate a 256-bit (32 bytes) key
    const array = new Uint8Array(32)
    crypto.getRandomValues(array)
    // Convert to base64 for easy storage and transmission
    return btoa(String.fromCharCode.apply(null, array))
  }

  /**
   * Store encryption key for a video
   * @param {string} videoId - The ID of the video
   * @param {string} encryptionKey - The encryption key to store
   */
  storeKey(videoId, encryptionKey) {
    if (!videoId || !encryptionKey) return

    try {
      const keys = this.getAllKeys()
      keys[videoId] = {
        key: encryptionKey,
        createdAt: new Date().toISOString(),
        lastUsed: new Date().toISOString()
      }
      localStorage.setItem(this.storageKey, JSON.stringify(keys))
    } catch (error) {
      console.error('Failed to store encryption key:', error)
    }
  }

  /**
   * Retrieve encryption key for a video
   * @param {string} videoId - The ID of the video
   * @returns {string|null} The encryption key or null if not found
   */
  getKey(videoId) {
    if (!videoId) return null

    try {
      const keys = this.getAllKeys()
      if (keys[videoId]) {
        // Update last used timestamp
        keys[videoId].lastUsed = new Date().toISOString()
        localStorage.setItem(this.storageKey, JSON.stringify(keys))
        return keys[videoId].key
      }
    } catch (error) {
      console.error('Failed to retrieve encryption key:', error)
    }
    return null
  }

  /**
   * Check if a key exists for a video
   * @param {string} videoId - The ID of the video
   * @returns {boolean} True if key exists
   */
  hasKey(videoId) {
    return this.getKey(videoId) !== null
  }

  /**
   * Remove encryption key for a video
   * @param {string} videoId - The ID of the video
   */
  removeKey(videoId) {
    if (!videoId) return

    try {
      const keys = this.getAllKeys()
      delete keys[videoId]
      localStorage.setItem(this.storageKey, JSON.stringify(keys))
    } catch (error) {
      console.error('Failed to remove encryption key:', error)
    }
  }

  /**
   * Get all stored encryption keys
   * @returns {Object} Object containing all stored keys
   */
  getAllKeys() {
    try {
      const keys = localStorage.getItem(this.storageKey)
      return keys ? JSON.parse(keys) : {}
    } catch (error) {
      console.error('Failed to parse stored keys:', error)
      return {}
    }
  }

  /**
   * Clear all stored encryption keys
   */
  clearAllKeys() {
    localStorage.removeItem(this.storageKey)
  }

  /**
   * Export keys for backup
   * @returns {string} JSON string of all keys
   */
  exportKeys() {
    return JSON.stringify(this.getAllKeys(), null, 2)
  }

  /**
   * Import keys from backup
   * @param {string} keysJson - JSON string of keys to import
   * @param {boolean} merge - Whether to merge with existing keys or replace
   */
  importKeys(keysJson, merge = true) {
    try {
      const importedKeys = JSON.parse(keysJson)
      if (merge) {
        const existingKeys = this.getAllKeys()
        const mergedKeys = { ...existingKeys, ...importedKeys }
        localStorage.setItem(this.storageKey, JSON.stringify(mergedKeys))
      } else {
        localStorage.setItem(this.storageKey, JSON.stringify(importedKeys))
      }
    } catch (error) {
      console.error('Failed to import keys:', error)
      throw new Error('Invalid keys format')
    }
  }

  /**
   * Get key statistics
   * @returns {Object} Statistics about stored keys
   */
  getStats() {
    const keys = this.getAllKeys()
    const videoIds = Object.keys(keys)
    
    return {
      totalKeys: videoIds.length,
      oldestKey: videoIds.reduce((oldest, id) => {
        const createdAt = new Date(keys[id].createdAt)
        return !oldest || createdAt < oldest ? createdAt : oldest
      }, null),
      newestKey: videoIds.reduce((newest, id) => {
        const createdAt = new Date(keys[id].createdAt)
        return !newest || createdAt > newest ? createdAt : newest
      }, null),
      lastUsed: videoIds.reduce((latest, id) => {
        const lastUsed = new Date(keys[id].lastUsed)
        return !latest || lastUsed > latest ? lastUsed : latest
      }, null)
    }
  }
}

// Export singleton instance
export default new EncryptionKeyService()