package com.example.data.local

import com.example.model.AspectRatioOption
import com.example.model.CameraMovement
import com.example.model.CameraShotType
import com.example.model.CharacterRole
import com.example.model.DialogueLanguage
import com.example.model.ProjectGenre
import com.example.model.SceneStatus
import com.example.model.SceneTransition
import com.example.model.VisualStyle
import com.example.model.VoiceMode

object DefaultSeedData {
    suspend fun seedIfEmpty(dao: StoryboardDao): Long? {
        if (dao.getProjectCount() > 0) return null

        val now = System.currentTimeMillis()
        val sampleIdea = "Kerajaan Semut kehabisan makanan menjelang musim hujan. Sireum Boy dan sahabatnya Kricik berpetualang mencari makanan, menemukan remah kue raksasa, mengatasi perebutan antarkelompok semut lewat kerja sama, hingga merayakan pesta sebelum bayangan musuh baru muncul."
        val sampleScript = """
            |SCENE 1: Kerajaan semut kehabisan makanan. Di aula bawah tanah yang megah, Raja Semut menatap lumbung yang kosong dengan cemas.
            |SCENE 2: Sireum Boy dan Kricik berangkat mencari makanan keluar terowongan dengan penuh semangat.
            |SCENE 3: Mereka menemukan remah kue cokelat raksasa berkilau di teras taman.
            |SCENE 4: Sekelompok semut liar datang berebut kue dengan tatapan serakah.
            |SCENE 5: Terjadi perebutan makanan hingga kue hampir jatuh ke selokan air.
            |SCENE 6: Sireum Boy melompat ke atas batu dan mengajak semua semut bekerja sama.
            |SCENE 7: Semua semut kompak mengangkat kue raksasa pulang melewati jembatan ranting.
            |SCENE 8: Pesta makanan meriah di Kerajaan Semut bermandikan cahaya lentera madu.
            |SCENE 9: Bayangan kumbang bertanduk bermata merah muncul mengintai di balik semak malam.
            |SCENE 10: Sireum Boy dan Kricik menatap fajar di puncak bukit, siap menghadapi petualangan baru.
        """.trimMargin()

        val projectId = dao.insertProject(
            ProjectEntity(
                title = "Sireum Boy — Mencari Makanan",
                genre = ProjectGenre.ANIMASI.name,
                aspectRatio = AspectRatioOption.RATIO_9_16.name,
                visualStyle = VisualStyle.CARTOON_2D_IN_3D_ENV.name,
                defaultSceneDurationSec = 10,
                targetTotalDurationSec = 100,
                dialogueLanguage = DialogueLanguage.INDONESIA.name,
                customLanguageName = "",
                voiceMode = VoiceMode.DIALOG_KARAKTER.name,
                subtitlesEnabled = false,
                storyIdea = sampleIdea,
                storyScript = sampleScript,
                projectReferenceImagePath = null,
                voiceOverVolume = 1.0f,
                dialogVolume = 1.0f,
                bgmVolume = 0.50f,
                sfxVolume = 0.50f,
                voiceOverMuted = false,
                dialogMuted = false,
                bgmMuted = false,
                sfxMuted = false,
                createdAt = now,
                updatedAt = now
            )
        )

        val sireumBoyId = dao.insertCharacter(
            CharacterEntity(
                projectId = projectId,
                characterCode = "CHR-001",
                role = CharacterRole.UTAMA.name,
                name = "Sireum Boy",
                faceShape = "Bulat oval anak laki-laki 10 tahun",
                bodyColor = "Kulit sawo matang cerah, rambut coklat muda berantakan",
                outfit = "Kostum superhero semut hitam-merah, cangkang semut merah di punggung",
                bodySize = "Anak laki-laki 10 tahun proporsional lincah",
                eyeShape = "Mata biru besar ekspresif",
                animationStyle = "Kartun 2D outline tinta hitam dengan shading sinematik",
                accessories = "Bandana antena semut hitam, sabuk mini",
                signatureTrait = "Senyum usil dan pintar, antena bergerak saat berpikir",
                description = "Anak laki-laki 10 tahun, rambut coklat muda berantakan, mata biru besar, ekspresi usil dan pintar, kostum superhero semut hitam-merah, ada antena, cangkang semut di punggung.",
                photoPath = null,
                bundledDrawableName = "img_char_sireum_boy"
            )
        )

        val kricikId = dao.insertCharacter(
            CharacterEntity(
                projectId = projectId,
                characterCode = "CHR-002",
                role = CharacterRole.PENDUKUNG.name,
                name = "Kricik",
                faceShape = "Bulat menggemaskan khas semut kartun",
                bodyColor = "Kuning cerah keemasan dengan garis tinta hitam",
                outfit = "Rompi penjelajah mini dari daun hijau",
                bodySize = "Semut kecil setinggi lutut Sireum Boy",
                eyeShape = "Mata bulat besar berkilau",
                animationStyle = "Kartun 2D outline tinta hitam yang ekspresif",
                accessories = "Tas selempang daun hijau mini",
                signatureTrait = "Selalu ceria dan melompat antusias",
                description = "Semut kuning kecil yang ceria, sahabat Sireum Boy, mata bulat besar ekspresif, antena kecil lincah, membawa tas daun mini.",
                photoPath = null,
                bundledDrawableName = "img_char_kricik"
            )
        )

        val bothChars = "$sireumBoyId,$kricikId"
        val sireumOnly = "$sireumBoyId"

        fun makeScene(
            idx: Int,
            title: String,
            visual: String,
            pos: String,
            expr: String,
            action: String,
            speaker: String,
            dialogue: String,
            narration: String,
            shot: CameraShotType,
            move: CameraMovement,
            transition: SceneTransition,
            location: String,
            lighting: String,
            props: String,
            mood: String,
            music: String,
            sfx: String,
            notes: String,
            chars: String
        ) = SceneEntity(
            projectId = projectId,
            orderIndex = idx,
            title = title,
            durationSec = 10,
            visualDescription = visual,
            characterPosition = pos,
            facialExpression = expr,
            characterAction = action,
            dialogueSpeaker = speaker,
            dialogueText = dialogue,
            narrationText = narration,
            cameraShot = shot.name,
            cameraMovement = move.name,
            transition = transition.name,
            location = location,
            lighting = lighting,
            importantProps = props,
            mood = mood,
            musicSfx = music,
            soundFx = sfx,
            notes = notes,
            status = SceneStatus.BELUM_ADA_GAMBAR.name,
            isApproved = false,
            videoGeneratedAt = 0L,
            characterIdsCsv = chars,
            customPrompt = "",
            customVideoPrompt = "",
            isPromptManuallyEdited = false,
            isVideoPromptManuallyEdited = false,
            referenceImagePath = null,
            variation1Path = null,
            variation2Path = null,
            selectedVariationIndex = 0,
            isVariationPlaceholder = false
        )

        val scenes = listOf(
            makeScene(
                0, "Kerajaan semut kehabisan makanan",
                "Aula bawah tanah Kerajaan Semut yang luas namun gudang makanannya kosong melompong. Raja Semut memandang piring kosong dengan cemas.",
                "Tengah latar belakang, menghadap keranjang kosong",
                "Cemas, alis berkerut penuh kekhawatiran",
                "Menghela napas dalam lalu menoleh perlahan ke arah warga semut",
                "Raja Semut", "Cadangan makanan kita hampir habis sebelum musim hujan tiba!",
                "Di kedalaman Kerajaan Semut, kepanikan terasa saat lumbung kosong.",
                CameraShotType.WIDE, CameraMovement.CRANE, SceneTransition.FADE,
                "Aula Kerajaan Semut bawah tanah bertekstur batu 3D",
                "Cahaya obor kristal hangat dengan bayangan dramatis dan depth of field",
                "Mahkota Raja Semut, keranjang lumbung kosong, obor kristal",
                "Tegang dan cemas", "Orkestra pelan bernada cemas", "Gema aula, bisik kerumunan semut",
                "Pastikan skala kerajaan terlihat megah dari sudut pandang semut.", sireumOnly
            ),
            makeScene(
                1, "Sireum Boy dan Kricik berangkat mencari makanan",
                "Sireum Boy berdiri gagah di gerbang terowongan dengan tangan di pinggang, didampingi Kricik yang melompat semangat membawa tas daunnya.",
                "Kiri dan kanan depan menghadap cahaya luar terowongan",
                "Senyum percaya diri, mata berbinar penuh tekad",
                "Mengambil ancang-ancang lalu melangkah maju sigap sambil menunjuk ke depan",
                "Sireum Boy", "Tenang, Kricik! Kita pasti temukan makanan paling enak hari ini!",
                "Berbekal keberanian, Sireum Boy dan Kricik melangkah keluar sarang.",
                CameraShotType.MEDIUM, CameraMovement.TRACKING, SceneTransition.SMOOTH_CINEMATIC,
                "Gerbang terowongan Kerajaan Semut menuju taman 3D",
                "Sinar matahari pagi menembus mulut terowongan dengan efek rim light",
                "Teropong mini dari ranting, tas daun Kricik",
                "Bersemangat dan petualang", "Musik petualangan ceria berirama drum", "Langkah kaki lincah, angin pagi",
                "Karakter harus konsisten sesuai Character ID.", bothChars
            ),
            makeScene(
                2, "Mereka menemukan remah kue raksasa",
                "Dari balik bilah rumput tinggi, Sireum Boy dan Kricik terbelalak kagum melihat potongan kue cokelat keemasan raksasa jatuh di lantai teras.",
                "Tengah bawah menyibak rumput, kue raksasa di latar atas",
                "Terbelalak takjub, mulut terbuka kagum",
                "Menyibak rumput dengan cepat lalu mencondongkan badan ke depan",
                "Kricik", "Wah! Kue itu lebih besar dari rumah kita, Sireum Boy!",
                "Tak jauh dari rerumputan, harta karun manis berkilau di bawah mentari.",
                CameraShotType.LOW_ANGLE, CameraMovement.DOLLY_IN, SceneTransition.SMOOTH_CINEMATIC,
                "Teras taman dekat rerumputan tinggi bertekstur 3D",
                "Cahaya keemasan pagi menyorot remah kue dengan bokeh latar belakang",
                "Remah kue cokelat raksasa dengan taburan gula",
                "Takjub dan penuh harapan", "Melodi ceria penuh keajaiban", "Suara kilau magis, kicau burung pagi",
                "Tunjukkan kontras ukuran kue raksasa dibanding mereka.", bothChars
            ),
            makeScene(
                3, "Semut lain datang berebut makanan",
                "Sekelompok semut pekerja liar bermunculan dari balik batu bata, menunjuk ke arah remah kue raksasa dengan wajah serakah.",
                "Sireum Boy & Kricik di kiri, kelompok semut liar di kanan",
                "Waspada dan terkejut menghadapi penantang",
                "Berbalik cepat mengambil posisi siap menjaga kue",
                "Pemimpin Semut Liar", "Hei! Kami yang melihat kue ini lebih dulu, pergi sana!",
                "Namun aroma manis kue itu mengundang kelompok semut lain.",
                CameraShotType.WIDE, CameraMovement.PAN_RIGHT, SceneTransition.CUT,
                "Teras taman di sekitar batu bata dan kue raksasa",
                "Kontras cahaya tegas menonjolkan ketegangan dua kubu",
                "Remah kue cokelat raksasa, batu bata taman",
                "Menegangkan dan ramai", "Musik ketegangan bertempo cepat", "Derap langkah semut berbaris cepat",
                "Posisi kue tetap di tengah komposisi.", bothChars
            ),
            makeScene(
                4, "Terjadi perebutan makanan",
                "Dua kelompok semut saling menarik remah kue ke arah berlawanan hingga remah kue bergoyang di tepi selokan air.",
                "Tersebar di kiri dan kanan menarik tali serat rumput",
                "Panik dan tegang berusaha menyeimbangkan kue",
                "Menarik tali sekuat tenaga lalu terpeleset kecil sebelum menahan pijakan",
                "Kricik", "Awas! Kalau ditarik terus, kuenya bisa jatuh ke air!",
                "Saling tarik tak terhindarkan, mengancam kue jatuh ke genangan air.",
                CameraShotType.TRACKING, CameraMovement.ORBIT, SceneTransition.CUT,
                "Tepi teras dekat selokan air dengan riak 3D",
                "Pencahayaan dinamis dengan pantulan cahaya air",
                "Tali serat rumput, remah kue raksasa miring di tepi",
                "Kacau dan mendesak", "Musik aksi cepat penuh ketegangan", "Suara tali menegang, remah kue bergeser, percikan air",
                "Gerakan kamera orbit menekankan bahaya jatuh.", bothChars
            ),
            makeScene(
                5, "Sireum Boy mengajak semua semut bekerja sama",
                "Sireum Boy melompat ke atas batu kecil, mengangkat tangan memberi aba-aba dengan senyum yakin sementara semua semut mendengarkan.",
                "Tengah atas di atas batu pijakan, menghadap kamera",
                "Tegas namun ramah, senyum penuh keyakinan",
                "Melompat lincah ke atas batu lalu membentangkan kedua tangan memberi aba-aba",
                "Sireum Boy", "Berhenti! Kuenya cukup buat semua! Kalau kita angkat bersama, pasti sampai rumah!",
                "Dengan suara lantang dan ide cerdas, Sireum Boy menyatukan semua semut.",
                CameraShotType.CLOSE_UP, CameraMovement.CLOSE_UP, SceneTransition.FADE,
                "Tepi teras di atas batu pijakan berlumut",
                "Cahaya heroik dari atas membentuk siluet tegas",
                "Batu kecil pijakan, remah kue di latar belakang",
                "Inspiratif dan hangat", "Musik heroik hangat yang mulai naik", "Suara angin tenang, hentakan kaki mantap",
                "Tonjolkan ekspresi pintar & ramah Sireum Boy.", bothChars
            ),
            makeScene(
                6, "Mereka bekerja sama membawa makanan pulang",
                "Puluhan semut bersama Sireum Boy dan Kricik berbaris rapi mengangkat kue raksasa, berjalan kompak melintasi jembatan ranting.",
                "Barisan kompak bergerak dari kiri ke kanan frame",
                "Gembira, semangat, saling menyemangati",
                "Berjalan serempak penuh irama sambil mengayunkan langkah ceria",
                "Semua Semut", "Satu, dua, angkat! Bersama kita kuat!",
                "Gotong royong membuat beban seberat apa pun terasa ringan.",
                CameraShotType.WIDE, CameraMovement.TRACKING, SceneTransition.SLIDE,
                "Jembatan ranting di atas taman bunga 3D",
                "Cahaya siang cerah hangat dengan depth of field lembut",
                "Jembatan ranting, kue cokelat raksasa terangkat bersama",
                "Kompak, ceria, dan penuh semangat", "Musik mars akustik ceria", "Nyanyian kompak semut, derap kaki berirama",
                "Tunjukkan Kricik di barisan depan memberi semangat.", bothChars
            ),
            makeScene(
                7, "Pesta makanan di kerajaan semut",
                "Aula Kerajaan Semut bermandikan cahaya lentera hangat. Semua semut, Sireum Boy, dan Kricik berpesta menikmati kue bersama Raja Semut.",
                "Tersebar meriah di sekitar meja daun raksasa",
                "Tertawa lebar penuh kebahagiaan",
                "Bersulang potongan kue sambil menari kecil penuh suka cita",
                "Kricik", "Hore! Ini kue paling lezat sepanjang sejarah Kerajaan Semut!",
                "Malam itu, Kerajaan Semut merayakan kebersamaan dengan pesta meriah.",
                CameraShotType.WIDE, CameraMovement.PAN_LEFT, SceneTransition.FADE,
                "Aula Pesta Kerajaan Semut penuh lentera madu",
                "Cahaya keemasan hangat dari ratusan lentera madu",
                "Lentera madu hangat, meja daun penuh potongan kue",
                "Bahagia, hangat, dan meriah", "Musik perayaan akustik ceria", "Sorak sorai gembira, tawa riang",
                "Nuansa hangat dan penuh kebersamaan.", bothChars
            ),
            makeScene(
                8, "Bayangan musuh kembali muncul",
                "Di luar gerbang Kerajaan Semut yang gelap, bayangan kumbang bertanduk bermata merah menyala mengintip dari balik semak malam.",
                "Tengah bayangan gelap di balik dedaunan",
                "Tatapan tajam mengancam dalam siluet",
                "Menyibak daun perlahan dengan gerakan mengendap-endap penuh misteri",
                "", "",
                "Namun di luar kegelapan malam, sepasang mata misterius mulai mengawasi.",
                CameraShotType.LOW_ANGLE, CameraMovement.DOLLY_IN, SceneTransition.CUT,
                "Semak gelap di luar Kerajaan Semut malam hari",
                "Sinar bulan biru dingin dengan mata merah menyala kontras",
                "Siluet kumbang bertanduk, sinar bulan purnama",
                "Misterius dan menegangkan", "Dentuman bass rendah misterius", "Suara gesekan daun, jangkrik mendadak sunyi",
                "Kontras dramatis dibanding pesta sebelumnya.", ""
            ),
            makeScene(
                9, "Petualangan belum berakhir",
                "Sireum Boy berdiri di puncak bukit tanah menghadap langit fajar keemasan, Kricik bertengger di bahunya menatap jauh ke depan.",
                "Tengah frame menghadap cakrawala fajar",
                "Senyum berani penuh optimisme",
                "Menempatkan tangan di pinggang, jubah berkibar dengan follow-through halus",
                "Sireum Boy", "Apa pun tantangan berikutnya, aku dan Kricik selalu siap!",
                "Satu misi selesai, namun petualangan baru Sireum Boy baru saja dimulai.",
                CameraShotType.MEDIUM, CameraMovement.CRANE, SceneTransition.FADE,
                "Puncak bukit tanah saat matahari terbit 3D",
                "Cahaya fajar keemasan spektakuler dengan lens flare lembut",
                "Langit fajar keemasan, jubah semut berkibar",
                "Heroik, penuh harapan, dan epik", "Tema musik heroik penutup megah", "Angin fajar berhembus pelan",
                "Shot penutup episode 1.", bothChars
            )
        )

        dao.insertScenes(scenes)
        return projectId
    }
}
