-- =============================================================================
-- Migration: 20260928010000_fix_avatar_storage_policies.sql
-- Description: Enforce strict user-isolated ownership on avatars storage bucket
-- Format required: <user-uuid>/avatar.jpg
-- =============================================================================

-- Ensure RLS is enabled on storage.objects
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

-- 1. Drop existing upload policy if present and recreate with strict user ownership check
DROP POLICY IF EXISTS "avatars_user_upload" ON storage.objects;
CREATE POLICY "avatars_user_upload" ON storage.objects
    FOR INSERT
    WITH CHECK (
        bucket_id = 'avatars' AND
        auth.uid() IS NOT NULL AND
        (auth.uid()::text = (storage.foldername(name))[1] OR auth.uid()::text = SPLIT_PART(name, '/', 1))
    );

-- 2. Drop existing update policy if present and recreate with strict user ownership check
DROP POLICY IF EXISTS "avatars_user_update" ON storage.objects;
CREATE POLICY "avatars_user_update" ON storage.objects
    FOR UPDATE
    USING (
        bucket_id = 'avatars' AND
        auth.uid() IS NOT NULL AND
        (auth.uid()::text = (storage.foldername(name))[1] OR auth.uid()::text = SPLIT_PART(name, '/', 1))
    )
    WITH CHECK (
        bucket_id = 'avatars' AND
        auth.uid() IS NOT NULL AND
        (auth.uid()::text = (storage.foldername(name))[1] OR auth.uid()::text = SPLIT_PART(name, '/', 1))
    );

-- 3. Drop existing delete policy if present and recreate with strict user ownership check
DROP POLICY IF EXISTS "avatars_user_delete" ON storage.objects;
CREATE POLICY "avatars_user_delete" ON storage.objects
    FOR DELETE
    USING (
        bucket_id = 'avatars' AND
        auth.uid() IS NOT NULL AND
        (auth.uid()::text = (storage.foldername(name))[1] OR auth.uid()::text = SPLIT_PART(name, '/', 1))
    );
