import { describe, expect, it } from 'vitest'
import { StorageConfig } from '@/utils/storage/storage-config'

/**
 * StorageConfig 单元测试：验证版本化存储键的生成、解析与匹配逻辑。
 */
describe('StorageConfig', () => {
  it('生成版本化存储键', () => {
    expect(StorageConfig.generateStorageKey('user')).toBe('sys-v1.0.0-user')
    expect(StorageConfig.generateStorageKey('user', '0.9.0')).toBe('sys-v0.9.0-user')
  })

  it('创建存储键匹配正则', () => {
    const pattern = StorageConfig.createKeyPattern('user')
    expect(pattern.test('sys-v1.0.0-user')).toBe(true)
    expect(pattern.test('sys-v1.0.0-setting')).toBe(false)
  })

  it('从存储键提取版本号与存储 ID', () => {
    expect(StorageConfig.extractVersionFromKey('sys-v1.0.0-user')).toBe('1.0.0')
    expect(StorageConfig.extractStoreIdFromKey('sys-v1.0.0-user')).toBe('user')
  })

  it('识别版本化键与当前版本键', () => {
    expect(StorageConfig.isVersionedKey('sys-v1.0.0-user')).toBe(true)
    expect(StorageConfig.isVersionedKey('plain-key')).toBe(false)
    expect(StorageConfig.isCurrentVersionKey('sys-v1.0.0-user')).toBe(true)
    expect(StorageConfig.isCurrentVersionKey('sys-v0.9.0-user')).toBe(false)
  })
})
