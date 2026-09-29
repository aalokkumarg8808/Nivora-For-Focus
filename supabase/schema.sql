-- =============================================================================
-- Plant: Focus & Study - PostgreSQL Database Schema for Supabase Cloud Sync
-- =============================================================================

-- Enable UUID extension if not already present
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- 1. PROFILES TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    photo_url TEXT,
    xp INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    total_focus_minutes INTEGER NOT NULL DEFAULT 0,
    completed_tasks INTEGER NOT NULL DEFAULT 0,
    completed_habits INTEGER NOT NULL DEFAULT 0,
    completed_goals INTEGER NOT NULL DEFAULT 0,
    theme_preference TEXT NOT NULL DEFAULT 'System',
    reduce_motion BOOLEAN NOT NULL DEFAULT FALSE,
    focus_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    task_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    habit_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    focus_sounds BOOLEAN NOT NULL DEFAULT TRUE,
    completion_sound BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- 2. TASKS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    date TEXT NOT NULL DEFAULT '',
    time TEXT NOT NULL DEFAULT '',
    reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_repeat TEXT NOT NULL DEFAULT 'Once',
    reminder_days JSONB NOT NULL DEFAULT '[]'::jsonb,
    category TEXT NOT NULL DEFAULT 'Study',
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- =============================================================================
-- 3. HABITS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.habits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    frequency TEXT NOT NULL DEFAULT 'Daily',
    category TEXT NOT NULL DEFAULT 'Health',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- =============================================================================
-- 4. HABIT LOGS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.habit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id UUID NOT NULL REFERENCES public.habits(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    date TEXT NOT NULL, -- Format: YYYY-MM-DD
    completed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_habit_date UNIQUE (habit_id, date)
);

-- =============================================================================
-- 5. GOALS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    deadline TEXT NOT NULL DEFAULT '',
    progress NUMERIC(5,2) NOT NULL DEFAULT 0.0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

-- =============================================================================
-- 6. MILESTONES TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES public.goals(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- 7. FOCUS SESSIONS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.focus_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    duration_minutes INTEGER NOT NULL DEFAULT 25,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed BOOLEAN NOT NULL DEFAULT TRUE,
    category TEXT NOT NULL DEFAULT 'Study',
    scene TEXT NOT NULL DEFAULT 'Forest',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- 8. ACHIEVEMENTS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    achievement_id TEXT NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_achievement UNIQUE (user_id, achievement_id)
);

-- =============================================================================
-- INDEXES FOR SYNC & HIGH-PERFORMANCE QUERYING
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_tasks_user_updated ON public.tasks (user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_tasks_user_deleted ON public.tasks (user_id, deleted_at);
CREATE INDEX IF NOT EXISTS idx_tasks_user_date ON public.tasks (user_id, date);

CREATE INDEX IF NOT EXISTS idx_habits_user_updated ON public.habits (user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_habits_user_deleted ON public.habits (user_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_habit_logs_user_date ON public.habit_logs (user_id, date);
CREATE INDEX IF NOT EXISTS idx_habit_logs_habit ON public.habit_logs (habit_id);

CREATE INDEX IF NOT EXISTS idx_goals_user_updated ON public.goals (user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_goals_user_deleted ON public.goals (user_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_milestones_goal ON public.milestones (goal_id);
CREATE INDEX IF NOT EXISTS idx_milestones_user ON public.milestones (user_id);

CREATE INDEX IF NOT EXISTS idx_focus_sessions_user_started ON public.focus_sessions (user_id, started_at);
CREATE INDEX IF NOT EXISTS idx_focus_sessions_user_updated ON public.focus_sessions (user_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_achievements_user ON public.achievements (user_id);

-- =============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =============================================================================

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.habit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.milestones ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.focus_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.achievements ENABLE ROW LEVEL SECURITY;

-- 1. Profiles RLS Policies
DROP POLICY IF EXISTS "profiles_select_own" ON public.profiles;
CREATE POLICY "profiles_select_own" ON public.profiles
    FOR SELECT USING (auth.uid() = id);

DROP POLICY IF EXISTS "profiles_insert_own" ON public.profiles;
CREATE POLICY "profiles_insert_own" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;
CREATE POLICY "profiles_update_own" ON public.profiles
    FOR UPDATE USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "profiles_delete_own" ON public.profiles;
CREATE POLICY "profiles_delete_own" ON public.profiles
    FOR DELETE USING (auth.uid() = id);

-- 2. Tasks RLS Policies
DROP POLICY IF EXISTS "tasks_select_own" ON public.tasks;
CREATE POLICY "tasks_select_own" ON public.tasks
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "tasks_insert_own" ON public.tasks;
CREATE POLICY "tasks_insert_own" ON public.tasks
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "tasks_update_own" ON public.tasks;
CREATE POLICY "tasks_update_own" ON public.tasks
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "tasks_delete_own" ON public.tasks;
CREATE POLICY "tasks_delete_own" ON public.tasks
    FOR DELETE USING (auth.uid() = user_id);

-- 3. Habits RLS Policies
DROP POLICY IF EXISTS "habits_select_own" ON public.habits;
CREATE POLICY "habits_select_own" ON public.habits
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "habits_insert_own" ON public.habits;
CREATE POLICY "habits_insert_own" ON public.habits
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "habits_update_own" ON public.habits;
CREATE POLICY "habits_update_own" ON public.habits
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "habits_delete_own" ON public.habits;
CREATE POLICY "habits_delete_own" ON public.habits
    FOR DELETE USING (auth.uid() = user_id);

-- 4. Habit Logs RLS Policies
DROP POLICY IF EXISTS "habit_logs_select_own" ON public.habit_logs;
CREATE POLICY "habit_logs_select_own" ON public.habit_logs
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "habit_logs_insert_own" ON public.habit_logs;
CREATE POLICY "habit_logs_insert_own" ON public.habit_logs
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "habit_logs_update_own" ON public.habit_logs;
CREATE POLICY "habit_logs_update_own" ON public.habit_logs
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "habit_logs_delete_own" ON public.habit_logs;
CREATE POLICY "habit_logs_delete_own" ON public.habit_logs
    FOR DELETE USING (auth.uid() = user_id);

-- 5. Goals RLS Policies
DROP POLICY IF EXISTS "goals_select_own" ON public.goals;
CREATE POLICY "goals_select_own" ON public.goals
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "goals_insert_own" ON public.goals;
CREATE POLICY "goals_insert_own" ON public.goals
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "goals_update_own" ON public.goals;
CREATE POLICY "goals_update_own" ON public.goals
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "goals_delete_own" ON public.goals;
CREATE POLICY "goals_delete_own" ON public.goals
    FOR DELETE USING (auth.uid() = user_id);

-- 6. Milestones RLS Policies
DROP POLICY IF EXISTS "milestones_select_own" ON public.milestones;
CREATE POLICY "milestones_select_own" ON public.milestones
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "milestones_insert_own" ON public.milestones;
CREATE POLICY "milestones_insert_own" ON public.milestones
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "milestones_update_own" ON public.milestones;
CREATE POLICY "milestones_update_own" ON public.milestones
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "milestones_delete_own" ON public.milestones;
CREATE POLICY "milestones_delete_own" ON public.milestones
    FOR DELETE USING (auth.uid() = user_id);

-- 7. Focus Sessions RLS Policies
DROP POLICY IF EXISTS "focus_sessions_select_own" ON public.focus_sessions;
CREATE POLICY "focus_sessions_select_own" ON public.focus_sessions
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "focus_sessions_insert_own" ON public.focus_sessions;
CREATE POLICY "focus_sessions_insert_own" ON public.focus_sessions
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "focus_sessions_update_own" ON public.focus_sessions;
CREATE POLICY "focus_sessions_update_own" ON public.focus_sessions
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "focus_sessions_delete_own" ON public.focus_sessions;
CREATE POLICY "focus_sessions_delete_own" ON public.focus_sessions
    FOR DELETE USING (auth.uid() = user_id);

-- 8. Achievements RLS Policies
DROP POLICY IF EXISTS "achievements_select_own" ON public.achievements;
CREATE POLICY "achievements_select_own" ON public.achievements
    FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "achievements_insert_own" ON public.achievements;
CREATE POLICY "achievements_insert_own" ON public.achievements
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "achievements_update_own" ON public.achievements;
CREATE POLICY "achievements_update_own" ON public.achievements
    FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "achievements_delete_own" ON public.achievements;
CREATE POLICY "achievements_delete_own" ON public.achievements
    FOR DELETE USING (auth.uid() = user_id);

-- =============================================================================
-- STORAGE BUCKET & POLICIES FOR AVATARS
-- =============================================================================

-- Ensure storage bucket exists
INSERT INTO storage.buckets (id, name, public)
VALUES ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

-- Storage RLS Policies for 'avatars' bucket (Format: <user_uuid>/avatar.jpg)
DROP POLICY IF EXISTS "avatars_public_read" ON storage.objects;
CREATE POLICY "avatars_public_read" ON storage.objects
    FOR SELECT USING (bucket_id = 'avatars');

DROP POLICY IF EXISTS "avatars_user_upload" ON storage.objects;
CREATE POLICY "avatars_user_upload" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'avatars' AND
        auth.uid() IS NOT NULL AND
        (auth.uid()::text = (storage.foldername(name))[1] OR auth.uid()::text = SPLIT_PART(name, '/', 1))
    );

DROP POLICY IF EXISTS "avatars_user_update" ON storage.objects;
CREATE POLICY "avatars_user_update" ON storage.objects
    FOR UPDATE USING (
        bucket_id = 'avatars' AND
        auth.uid() IS NOT NULL AND
        (auth.uid()::text = (storage.foldername(name))[1] OR auth.uid()::text = SPLIT_PART(name, '/', 1))
    );

DROP POLICY IF EXISTS "avatars_user_delete" ON storage.objects;
CREATE POLICY "avatars_user_delete" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'avatars' AND
        auth.uid() IS NOT NULL AND
        (auth.uid()::text = (storage.foldername(name))[1] OR auth.uid()::text = SPLIT_PART(name, '/', 1))
    );

-- =============================================================================
-- AUTOMATIC PROFILE CREATION ON AUTH SIGNUP
-- =============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, name)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'name', SPLIT_PART(NEW.email, '@', 1))
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
