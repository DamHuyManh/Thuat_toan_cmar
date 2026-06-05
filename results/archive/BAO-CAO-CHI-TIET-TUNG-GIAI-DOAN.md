# Báo Cáo Chi Tiết Theo Từng Giai Đoạn — Cài Đặt Thuật Toán CMAR

**Bài báo gốc:** Li, Han, Pei. *CMAR: Accurate and Efficient Classification Based on Multiple Class-Association Rules*. ICDM 2001.
**Ngôn ngữ cài đặt:** Java thuần.
**Dữ liệu kiểm thử:** 26 dataset chuẩn từ UCI Machine Learning Repository.
**Mục đích tài liệu:** Trình bày tỉ mỉ từng giai đoạn thực hiện, kèm ví dụ số liệu cụ thể.

---

## Mục Lục

- Giai đoạn 0 — Chuẩn bị dữ liệu
- Giai đoạn 1 — Đọc và phân tích CSV
- Giai đoạn 2 — Chia 10-fold Cross-Validation
- Giai đoạn 3 — Rời rạc hoá MDL
- Giai đoạn 4 — Mã hoá thành transaction
- Giai đoạn 5 — Khai phá luật bằng FP-Growth
- Giai đoạn 6 — Tỉa luật qua 3 tầng
- Giai đoạn 7 — Xây CR-Tree
- Giai đoạn 8 — Phân lớp mẫu test
- Giai đoạn 9 — Tính accuracy và tổng hợp
- Giai đoạn 10 — Xuất báo cáo

---

## Giai đoạn 0 — Chuẩn Bị Dữ Liệu

### Nguồn dữ liệu
Toàn bộ 26 dataset được tải từ **UCI Machine Learning Repository** — kho dữ liệu chuẩn dùng rộng rãi trong các bài báo nghiên cứu về phân lớp. Việc dùng UCI đảm bảo kết quả có thể so sánh trực tiếp với paper gốc của Li, Han, Pei.

### Cấu trúc thư mục dữ liệu
Em tổ chức dữ liệu thành ba thư mục tách biệt để đảm bảo tính minh bạch và có thể kiểm tra lại bất cứ lúc nào.

Thư mục `datasets_uci_raw/` giữ bản tải gốc từ UCI, không chỉnh sửa. Thư mục `datasets_csv/` chứa bản trung gian trong quá trình chuyển đổi định dạng. Thư mục `datasets/` là bản CSV cuối cùng đã được chuẩn hoá về cùng một định dạng nhưng **giữ nguyên mọi giá trị dữ liệu** — chỉ đổi cách trình bày (dấu phân tách, thứ tự cột), không thay đổi nội dung.

### Quy ước định dạng CSV
Mỗi file CSV tuân theo quy ước đơn giản: không có header, mỗi dòng là một bản ghi, các trường phân tách bằng dấu phẩy, **cột cuối cùng luôn là nhãn lớp**. Giá trị thiếu được biểu diễn bằng dấu hỏi `?` hoặc ô trống.

Ví dụ một đoạn đầu của file `iris.csv`: dòng đầu tiên có năm trường là `5.1`, `3.5`, `1.4`, `0.2`, `Iris-setosa` — trong đó bốn số đầu là các đặc trưng hình thái của hoa (chiều dài đài, chiều rộng đài, chiều dài cánh, chiều rộng cánh), và trường cuối cùng là loài hoa.

### Nguyên tắc quan trọng
Dữ liệu gốc **không bao giờ bị sửa đổi**. Mọi biến đổi (rời rạc hoá các thuộc tính số, mã hoá category, xử lý giá trị thiếu) đều chỉ diễn ra trong bộ nhớ khi chương trình chạy, và bị xoá khi chương trình kết thúc. File trên đĩa luôn nguyên vẹn.

---

## Giai đoạn 1 — Đọc Và Phân Tích CSV

**Module phụ trách:** [DataLoader.java](../src/cmar/benchmark/DataLoader.java)

### Đọc file
Chương trình mở file CSV, đọc từng dòng một. Các dòng trống và dòng bắt đầu bằng ký tự đặc biệt (`#`, `@`, `%` — thường là comment trong định dạng ARFF hay một số biến thể) sẽ bị bỏ qua. Mỗi dòng hợp lệ được tách ra thành một mảng chuỗi theo dấu phẩy.

Kết quả của bước này là một bảng dữ liệu thô dạng chuỗi, chưa hiểu ý nghĩa của từng cột.

### Phát hiện kiểu thuộc tính
Với mỗi cột (trừ cột nhãn lớp ở cuối), chương trình thử chuyển toàn bộ giá trị sang dạng số thực. Nếu tất cả các giá trị đều chuyển được (bỏ qua những ô thiếu dữ liệu) thì cột đó được coi là **thuộc tính số** — sẽ cần rời rạc hoá bằng MDL ở giai đoạn sau. Ngược lại, nếu có bất kỳ giá trị nào không phải số (ví dụ `sunny`, `overcast`, `yes`, `no`) thì cột đó là **thuộc tính category** — sẽ được mã hoá trực tiếp thành số nguyên.

Ví dụ trên dataset Iris: bốn cột đầu đều là số đo hình thái nên là thuộc tính số, cột cuối chứa tên loài hoa nên là categorical (đóng vai trò nhãn lớp).

### Xử lý giá trị thiếu
Khi gặp giá trị `?` hoặc ô trống, em gán tạm cho chúng một nhãn đặc biệt là `"MISS"` trong bộ nhớ. Đây được coi như một category hợp lệ chứ không bị xoá đi.

Quyết định giữ giá trị thiếu thay vì bỏ có lý do cụ thể: với các dataset y tế như Hepatitis hay Horse, việc bác sĩ không đo được một chỉ số tự thân đã là thông tin có ý nghĩa. Ví dụ một bệnh nhân quá yếu đến mức không thể làm xét nghiệm albumin thường có tiên lượng xấu hơn — nếu xoá dòng này đi hoặc điền một giá trị trung bình giả, em sẽ mất thông tin quý giá đó.

Cụ thể, nếu dòng dữ liệu Hepatitis gốc có dạng *"tuổi 60, giới tính nam, steroid 1, antivirals 2, fatigue thiếu, malaise 2, albumin 1.0, LIVE"*, thì trong bộ nhớ giá trị fatigue bị thiếu sẽ trở thành category `"MISS"`. File gốc vẫn giữ dấu `?`.

### Mã hoá nhãn lớp
Các giá trị nhãn lớp (dạng chuỗi) được ánh xạ sang số nguyên theo thứ tự xuất hiện. Với Iris, `Iris-setosa` nhận giá trị 0, `Iris-versicolor` nhận 1, `Iris-virginica` nhận 2. Ánh xạ này được lưu lại để sau này có thể giải mã ngược nếu cần.

### Kết thúc giai đoạn 1
Sau giai đoạn này, chương trình có được một bảng dữ liệu đã được phân tích: biết cột nào là số, cột nào là category, nhãn lớp đã được mã hoá thành số nguyên, và các giá trị thiếu đã được đánh dấu rõ ràng.

---

## Giai đoạn 2 — Chia 10-Fold Cross-Validation

**Module phụ trách:** [BenchmarkRunner.java](../src/cmar/benchmark/BenchmarkRunner.java)

### Tại sao cần chia 10 fold?
Nếu chỉ chia dataset thành một cặp train/test cố định, kết quả sẽ phụ thuộc may rủi vào cách chia. 10-fold cross-validation giải quyết bằng cách chia dataset thành 10 phần, lần lượt lấy mỗi phần làm test, 9 phần còn lại làm train, rồi lấy trung bình 10 lần chạy. Cách này cho ước lượng accuracy ổn định hơn và được dùng làm chuẩn trong hầu hết bài báo về phân lớp.

### Stratified split — giữ tỉ lệ class
Một vấn đề có thể xảy ra khi chia ngẫu nhiên: nếu fold nào đó tình cờ có rất ít mẫu thuộc lớp hiếm, model sẽ học lệch. Stratified split khắc phục bằng cách **giữ nguyên tỉ lệ các lớp trong mỗi fold** giống như tỉ lệ của toàn dataset.

Quy trình cụ thể: chương trình nhóm các bản ghi theo lớp, xáo trộn ngẫu nhiên trong mỗi nhóm (với seed cố định để tái lập được), rồi chia mỗi nhóm thành 10 phần bằng nhau. Fold thứ k cuối cùng là tập hợp phần thứ k của mọi nhóm lớp.

Ví dụ với Iris có 150 mẫu chia đều 3 lớp (mỗi lớp 50 mẫu): mỗi lớp được chia thành 10 phần nhỏ 5 mẫu. Fold 1 gồm 5 mẫu setosa + 5 mẫu versicolor + 5 mẫu virginica, tổng 15 mẫu, tỉ lệ 1:1:1 đúng như dataset gốc. Các fold khác cũng vậy.

### Vòng lặp đánh giá
Chương trình lặp 10 lần. Ở lần thứ k, fold thứ k được dùng làm tập test, 9 fold còn lại ghép lại làm tập train. Thuật toán CMAR huấn luyện trên tập train rồi dự đoán trên tập test để đo accuracy. Sau 10 vòng lặp, em lấy trung bình 10 accuracy để làm kết quả cuối cùng cho dataset đó.

### Tính tái lập được
Seed ngẫu nhiên được đặt cố định là 42. Nghĩa là chạy chương trình bao nhiêu lần cũng ra cùng một kết quả. Điều này quan trọng khi cần kiểm chứng lại kết quả hoặc khi so sánh các phiên bản khác nhau của thuật toán.

---

## Giai đoạn 3 — Rời Rạc Hoá MDL

**Module phụ trách:** [MDLDiscretizer.java](../src/cmar/MDLDiscretizer.java)

### Tại sao cần rời rạc hoá?
Luật kết hợp chỉ hiểu được các điều kiện dạng rời rạc như *"màu = xanh"* hoặc *"tuổi thuộc nhóm cao"*. Không thể viết một luật dạng *"tuổi = 37.5"* vì xác suất có hai người cùng đúng 37.5 tuổi là rất thấp — luật đó sẽ không bao giờ khớp ai trong tập test. Vì vậy các thuộc tính số phải được chia thành vài nhóm (bin), ví dụ *"tuổi thấp"*, *"tuổi trung bình"*, *"tuổi cao"*.

### Nguyên lý thuật toán Fayyad-Irani
Thuật toán rời rạc hoá thông minh là Fayyad-Irani MDL. Ý tưởng gốc rất tự nhiên: muốn chia một thuộc tính số thành các khoảng, ta chọn điểm cắt sao cho **mỗi khoảng càng thuần về lớp càng tốt** — nghĩa là mỗi khoảng chứa chủ yếu một lớp duy nhất, không lẫn lộn.

Để đo độ "thuần", em dùng khái niệm **entropy** trong lý thuyết thông tin. Entropy bằng 0 nghĩa là tập chỉ có một lớp (thuần tuyệt đối), entropy cao nghĩa là các lớp trộn đều (hỗn loạn).

### Quy trình tìm điểm cắt
Chương trình sắp các giá trị của thuộc tính theo thứ tự tăng dần, rồi thử mọi điểm cắt khả thi (giữa hai giá trị liền kề khác nhau). Với mỗi điểm cắt tiềm năng, chia dữ liệu thành hai nửa trái-phải và tính **information gain** — đó là mức giảm entropy khi cắt. Gain càng cao, điểm cắt càng tốt.

Sau khi tìm được điểm cắt có gain cao nhất, chương trình áp một tiêu chí gọi là **MDL (Minimum Description Length)** để quyết định có nên thực sự cắt hay không. Nếu gain không đủ lớn so với một ngưỡng phụ thuộc kích thước dữ liệu, chương trình kết luận "cắt không đáng" và dừng lại. Cơ chế này giúp tránh cắt quá mức (overfitting) gây ra các bin quá nhỏ không có ý nghĩa thống kê.

Nếu chấp nhận cắt, thuật toán đệ quy áp dụng cùng quy trình lên hai nửa vừa tạo ra, cho đến khi không còn điểm cắt nào đáng giá.

### Ví dụ minh hoạ
Giả sử có 7 bệnh nhân với thuộc tính "tuổi" lần lượt là 22, 25, 28, 35, 42, 50, 58, và nhãn lớp tương ứng là A, A, A, B, B, B, B. Nhìn bằng mắt thường, ta thấy có một ranh giới khá rõ: ba người trẻ nhất (22, 25, 28) đều thuộc lớp A, bốn người còn lại (35, 42, 50, 58) đều thuộc lớp B.

Khi thử các điểm cắt, chương trình sẽ tính toán và thấy rằng điểm cắt tại 31.5 (giữa 28 và 35) cho information gain cao nhất — vì nó tách hoàn hảo hai lớp. Tiêu chí MDL chấp nhận điểm cắt này. Sau đó đệ quy vào nửa trái (chỉ còn lớp A, entropy bằng 0, dừng) và nửa phải (chỉ còn lớp B, entropy bằng 0, dừng).

Kết quả cuối: thuộc tính tuổi được chia thành hai nhóm — "tuổi ≤ 31.5" và "tuổi > 31.5".

### Điểm mấu chốt chống data leakage
Đây là chi tiết quan trọng nhất trong toàn bộ pipeline. Việc tìm điểm cắt **chỉ được thực hiện trên tập train của fold hiện tại**, tuyệt đối không nhìn vào tập test. Sau khi đã có điểm cắt, chương trình mới áp chúng vào cả train và test để biến giá trị số thành chỉ số bin.

Nếu làm ngược lại (rời rạc hoá trên toàn bộ dataset trước rồi mới chia fold), test fold đã "biết trước" điểm cắt — và điểm cắt có phần đóng góp từ chính nó. Đó là data leakage, khiến accuracy đo được cao hơn thực tế khoảng 1%. Đây là lỗi em đã phát hiện và sửa trong quá trình thực hiện.

---

## Giai đoạn 4 — Mã Hoá Thành Transaction

### Tại sao cần mã hoá?
Sau khi rời rạc hoá, mỗi thuộc tính có một số hữu hạn giá trị rời rạc (ví dụ "tuổi" có 2 bin, "giới tính" có 2 category, "nghề nghiệp" có 10 category). Để thuật toán FP-Growth xử lý, em cần biến mỗi cặp (thuộc tính, giá trị) thành một **item_id duy nhất** dạng số nguyên.

### Cách cấp item_id
Em duyệt lần lượt từng thuộc tính và cấp một dải số nguyên liên tiếp cho các giá trị của thuộc tính đó. Ví dụ thuộc tính đầu tiên có 3 bin sẽ nhận các item 0, 1, 2. Thuộc tính thứ hai có 2 giá trị sẽ nhận item 3, 4. Thuộc tính thứ ba có 4 bin sẽ nhận item 5 đến 8, và cứ thế tiếp tục. Không có hai cặp (thuộc tính, giá trị) nào chia sẻ cùng một item_id.

### Biến bản ghi thành transaction
Mỗi bản ghi sau khi mã hoá trở thành một tập số nguyên gọi là transaction — giống như một "giỏ hàng" trong các bài toán association rule kinh điển. Ví dụ bản ghi Iris với bốn thuộc tính rơi vào các bin lần lượt là (bin 2, bin 1, bin 0, bin 0) sẽ trở thành transaction gồm các item {2, 4, 5, 9} và có nhãn lớp là 0.

### Tại sao dùng số nguyên thay vì chuỗi?
Có ba lý do chính. Thứ nhất, so sánh số nguyên nhanh hơn so sánh chuỗi nhiều lần. Thứ hai, em có thể dùng biểu diễn bitmap (mỗi bit tương ứng một item có/không xuất hiện), khi đó việc kiểm tra hai tập khớp nhau quy về một phép AND bit — cực nhanh. Thứ ba, tiết kiệm bộ nhớ đáng kể so với lưu chuỗi.

---

## Giai đoạn 5 — Khai Phá Luật Bằng FP-Growth

**Module phụ trách:** [FPGrowth.java](../src/cmar/FPGrowth.java), [FPTree.java](../src/cmar/FPTree.java)

### Mục tiêu
Tìm mọi tập item **xuất hiện đủ thường xuyên** trong dữ liệu train (gọi là frequent itemset), rồi từ đó sinh ra các luật dạng "tập item → lớp" có độ tin cậy đủ cao.

### Tại sao không dùng Apriori?
Apriori là thuật toán truyền thống cho bài toán association rule, nhưng nó phải quét toàn bộ cơ sở dữ liệu nhiều lần (mỗi vòng lặp một lần để đếm tần suất của itemset độ dài k). Với dataset lớn như Waveform 5000 mẫu hay với itemset dài, Apriori trở nên quá chậm. FP-Growth (do Jiawei Han đề xuất) chỉ cần quét dữ liệu **hai lần**, sau đó mọi thao tác khai phá diễn ra trên một cấu trúc cây nén gọi là FP-Tree — nhanh hơn Apriori thường là hàng chục lần.

### Bước 1: Đếm tần suất item
Lần quét thứ nhất: chương trình đi qua toàn bộ transaction và đếm mỗi item xuất hiện bao nhiêu lần. Các item có tần suất dưới ngưỡng `min_sup` (thường là 1% số transaction) bị loại vì chúng quá hiếm để tạo nên luật ý nghĩa.

### Bước 2: Xây FP-Tree
Lần quét thứ hai: với mỗi transaction, chương trình sắp xếp các item trong đó theo tần suất giảm dần (item phổ biến nhất ở đầu), rồi chèn vào cây. Các transaction có tiền tố chung sẽ **chia sẻ cùng một nhánh của cây**, giúp nén dữ liệu đáng kể.

Mỗi node trên cây lưu một item kèm counter. Ngoài ra có một "header table" liệt kê mọi item, và với mỗi item, một liên kết dạng danh sách móc nối tất cả node trên cây chứa item đó — giúp truy cập nhanh khi khai phá.

Hình dung trực quan: nếu có 1000 khách hàng đều mua "sữa và bánh mì", thay vì lưu 1000 lần, FP-Tree chỉ lưu một nhánh "sữa → bánh mì" với counter bằng 1000.

### Bước 3: Khai phá đệ quy
Chương trình xét lần lượt từng item trong header table, bắt đầu từ item **ít phổ biến nhất** (đây là thứ tự chuẩn của FP-Growth). Với mỗi item, chương trình đi ngược từ mọi vị trí của item đó lên gốc cây, thu được một tập các đường đi gọi là "conditional pattern base". Từ các đường đi này, chương trình xây một FP-Tree con nhỏ hơn và đệ quy khai phá tiếp. Mọi tập prefix hình thành trong quá trình đệ quy chính là một frequent itemset.

Có một tối ưu đáng kể: nếu FP-Tree chỉ có đúng một đường đi duy nhất (single path), thì mọi tổ hợp con của các item trên đường đó đều là frequent itemset, không cần đệ quy nữa — chỉ cần liệt kê tổ hợp.

### Bước 4: Sinh luật từ frequent itemset
Với mỗi frequent itemset I tìm được, chương trình duyệt lại dữ liệu train để đếm: trong số các transaction chứa I, mỗi lớp chiếm bao nhiêu. Từ đó tính confidence cho mỗi lớp: `confidence = số transaction chứa I và thuộc lớp c / số transaction chứa I`. Nếu support và confidence đều vượt ngưỡng, em tạo ra một luật "I → c" và lưu lại.

### Các giới hạn an toàn
Vì số luật có thể rất lớn, em đặt một số cận để tránh tràn bộ nhớ và treo chương trình: tối đa 5 triệu itemset, tối đa 10 phút thời gian khai phá, tối đa 80.000 luật cho mỗi lớp, và antecedent (vế trái) không được dài quá 6 item. Trên thực tế các ngưỡng này hiếm khi chạm đến với dataset UCI kích thước trung bình.

### Con số minh hoạ
Trên Iris với 135 transaction huấn luyện (sau khi chia fold), FP-Growth thường sinh ra khoảng 50 frequent itemset và 30-80 luật. Trên dataset lớn hơn như Waveform, số luật có thể lên đến vài chục nghìn trước khi tỉa.

---

## Giai đoạn 6 — Tỉa Luật Qua 3 Tầng

**Module phụ trách:** [RulePruner.java](../src/cmar/RulePruner.java)

Số luật sinh ra ở giai đoạn trước có thể rất lớn — nhiều luật trong đó là dư thừa, ngẫu nhiên, hoặc quá cụ thể. Paper đề xuất ba tầng tỉa liên tiếp để giữ lại tập luật gọn gàng nhưng vẫn đủ mạnh.

### Tầng 1 — Tỉa bằng Chi-Square
Mục đích của tầng này là loại những luật **không có ý nghĩa thống kê** — tức là luật có thể chỉ đúng do may rủi chứ không phản ánh quy luật thực.

Kiểm định chi-square là công cụ thống kê chuẩn để xét xem hai biến (ở đây là "có tập item A" và "thuộc lớp c") có liên hệ thực sự với nhau hay không. Nếu liên hệ yếu, giá trị chi-square thấp; nếu liên hệ mạnh, chi-square cao.

Ngưỡng `3.8415` được chọn vì đây là giá trị tới hạn của phân phối chi-square với 1 bậc tự do ở mức ý nghĩa 95% — chuẩn thống kê phổ biến. Luật nào có chi-square thấp hơn ngưỡng này bị coi là không đáng tin và bị loại. Ngoài ra, em cũng yêu cầu confidence của luật phải lớn hơn xác suất tiền nghiệm của lớp (tỉ lệ lớp đó trong toàn train) — để đảm bảo luật mang thông tin mới chứ không phải chỉ lặp lại phân bố lớp tự nhiên.

Một ví dụ cụ thể: xét luật *"chiều dài cánh hoa thuộc bin 2 → Iris-versicolor"* trên Iris với 135 mẫu train, trong đó 45 mẫu là versicolor. Giả sử tập train có 44 mẫu thoả điều kiện vế trái, và 42 trong số đó thực sự là versicolor. Tính toán chi-square trên bảng tiếp liên 2×2 cho ra khoảng 105. Confidence của luật là 42/44 ≈ 0.955, cao hơn nhiều so với xác suất tiền nghiệm 45/135 ≈ 0.33. Chi-square 105 vượt xa ngưỡng 3.84 nên luật được giữ lại.

### Tầng 2 — Tỉa general-to-specific
Tầng này loại các luật **dư thừa theo nghĩa bao hàm**. Nếu một luật tổng quát đã mạnh, thì luật cụ thể hơn (bổ sung thêm điều kiện nhưng không cải thiện confidence) là dư thừa và nên bỏ.

Ví dụ trực quan: giả sử em có hai luật cùng dự đoán "ở nhà". Luật thứ nhất là *"trời mưa thì ở nhà"* với confidence 0.90. Luật thứ hai là *"trời mưa và lạnh thì ở nhà"* với confidence 0.88. Luật thứ hai đặc biệt hơn (thêm điều kiện "lạnh") nhưng confidence lại thấp hơn — nghĩa là thêm điều kiện không giúp gì, chỉ làm luật khó khớp hơn. Trong trường hợp này, luật thứ hai bị loại.

Về mặt cài đặt, em sắp luật theo độ dài vế trái tăng dần, duyệt từ luật tổng quát đến luật cụ thể. Với mỗi luật cụ thể, kiểm tra xem có luật tổng quát hơn cùng lớp với confidence cao hơn không — nếu có thì loại luật cụ thể. Bước này có độ phức tạp O(n²) nên em bỏ qua nếu số luật vượt quá 10.000 để tránh chậm.

### Tầng 3 — Database Coverage Pruning
Tầng này là quan trọng nhất, giúp giảm số luật đáng kể mà vẫn đảm bảo khả năng phân lớp tập train.

Ý tưởng: nếu 50 luật đầu tiên đã phân lớp đúng mọi mẫu train, thì việc giữ thêm 950 luật nữa là lãng phí. Nhưng để đảm bảo độ bền (không phụ thuộc quá nhiều vào 1 luật), em yêu cầu **mỗi mẫu train phải được phủ bởi ít nhất δ luật** (em dùng δ = 4).

Thuật toán duyệt các luật đã sắp xếp theo thứ tự ưu tiên của CMAR (confidence giảm dần, rồi support, rồi độ ngắn). Mỗi mẫu train có một biến đếm "đã được bao nhiêu luật đúng phủ", khởi tạo bằng 0. Duyệt từng luật: nếu luật này phân lớp đúng ít nhất một mẫu chưa được phủ đủ δ lần, thì giữ luật, đồng thời tăng biến đếm cho mọi mẫu mà luật khớp. Khi biến đếm của một mẫu đạt δ, mẫu đó được coi là "phủ đầy" và không còn tác động đến quyết định giữ luật nữa.

Kết quả thực tế: trên dataset Anneal, số luật giảm từ khoảng 5000 xuống còn 200 sau tầng này — gấp 25 lần — nhưng accuracy không giảm mà thậm chí còn nhỉnh hơn (do ít overfitting).

### Tổng kết hiệu ứng tỉa
Trên một dataset trung bình, ba tầng tỉa kế tiếp nhau thường đưa số luật từ cỡ 10.000 xuống cỡ vài trăm. Tập luật cuối gọn, có ý nghĩa thống kê, không dư thừa, và đủ để phân lớp chính xác.

---

## Giai đoạn 7 — Xây CR-Tree

**Module phụ trách:** [CRTree.java](../src/cmar/CRTree.java)

### Mục đích
Khi dự đoán một mẫu test mới, chương trình phải tìm nhanh mọi luật trong tập luật đã tỉa mà khớp với mẫu đó. Nếu duyệt tuyến tính qua từng luật một, mỗi lần dự đoán mất O(số luật × độ dài antecedent) — chậm khi có nhiều mẫu test. CR-Tree là cấu trúc chỉ mục giúp tăng tốc bước này.

### Cấu trúc prefix-tree
CR-Tree là một cây tiền tố (prefix-tree). Mỗi luật được chèn theo đường đi từ gốc xuống, mỗi node trên đường đi tương ứng một item trong vế trái của luật. Luật được lưu tại node cuối cùng.

Lợi thế: hai luật có tiền tố giống nhau sẽ chia sẻ cùng nhánh đầu. Ví dụ luật `{item2, item5, item9}` và luật `{item2, item5, item11}` cùng đi qua nhánh `gốc → 2 → 5`, sau đó mới rẽ. Khi tìm luật khớp mẫu test, em chỉ cần kiểm tra điều kiện chung một lần ở phần tiền tố, thay vì lặp lại cho từng luật.

### Tính trọng số WCS
Trước khi chèn vào CR-Tree, mỗi luật được gán một trọng số gọi là Weighted Chi-Square (WCS). Đây là đại lượng sẽ được dùng khi "bỏ phiếu" trong giai đoạn phân lớp.

Trọng số được tính theo công thức của paper: bình phương của chi-square chia cho một đại lượng gọi là "max chi-square" — đây là **cận trên lý thuyết** của chi-square cho luật đó. Việc chuẩn hoá bằng max chi-square có ý nghĩa quan trọng: nó loại bỏ thiên lệch theo kích thước lớp. Nếu không chuẩn hoá, các luật gắn với lớp phổ biến (support lớn) sẽ tự nhiên có chi-square cao hơn và áp đảo bất công khi bỏ phiếu.

### Kết thúc pha huấn luyện
Sau giai đoạn 7, mô hình đã sẵn sàng. Chương trình nắm giữ: một CR-Tree chứa các luật đã tỉa kèm trọng số WCS, và một giá trị `defaultClass` là lớp đa số trong train (dùng làm dự đoán dự phòng khi không luật nào khớp mẫu test).

---

## Giai đoạn 8 — Phân Lớp Mẫu Test

**Module phụ trách:** [CMARClassifier.java](../src/cmar/CMARClassifier.java)

### Biểu diễn mẫu test
Mẫu test cũng được đưa qua các bước tiền xử lý giống train: áp các điểm cắt MDL đã học (để rời rạc hoá thuộc tính số), mã hoá thành item. Sau đó em chuyển tập item của mẫu sang dạng **bitmap** — một mảng long trong đó mỗi bit biểu thị item tương ứng có xuất hiện trong mẫu hay không. Bitmap cho phép kiểm tra khớp luật bằng phép AND bit cực nhanh.

### Tìm luật khớp
Chương trình duyệt CR-Tree từ gốc. Tại mỗi node, nó kiểm tra bit tương ứng trong bitmap của mẫu test. Nếu bit bằng 1 (nghĩa là item đó có trong mẫu), chương trình đi tiếp vào các con của node, đồng thời thu thập các luật được lưu tại node hiện tại. Nếu bit bằng 0, nhánh con đó bị cắt hoàn toàn. Kết quả là một danh sách tất cả luật có vế trái là tập con của mẫu test.

### Logic phân lớp hai bước
Em áp dụng đúng quy trình phân lớp mô tả trong paper CMAR, gồm hai bước nối tiếp.

**Bước một — kiểm tra đồng thuận top-confidence.** Trong danh sách luật khớp, em tìm các luật có confidence cao nhất. Nếu tất cả các luật top-confidence này **cùng dự đoán một lớp**, thì em trả về ngay lớp đó mà không cần bước hai. Ý nghĩa: khi nhiều luật mạnh nhất đều đồng ý, không cần tranh luận thêm.

**Bước hai — voting có trọng số.** Nếu các luật top-confidence chia rẽ giữa nhiều lớp, chương trình chuyển sang tính điểm: với mỗi lớp, em cộng tổng trọng số WCS của tất cả luật khớp trỏ về lớp đó. Lớp nào có tổng điểm cao nhất được chọn làm kết quả.

### Ví dụ minh hoạ
Giả sử một mẫu test Iris có 4 luật khớp trong CR-Tree. Luật thứ nhất confidence 0.95, dự đoán versicolor. Luật thứ hai và thứ ba lần lượt có confidence 0.88 và 0.85, đều dự đoán versicolor. Luật thứ tư confidence 0.50, dự đoán setosa. Ở bước một, confidence cao nhất là 0.95 — chỉ có một luật đạt, và luật đó dự đoán versicolor. Mọi luật top-confidence đồng thuận versicolor → trả về versicolor luôn, không cần bước hai.

Trong một tình huống khác, nếu hai luật cùng có confidence cao nhất 0.95 nhưng dự đoán hai lớp khác nhau, chương trình phải sang bước hai và cộng điểm của mọi luật khớp để quyết định.

### Trường hợp không luật nào khớp
Đôi khi mẫu test rơi vào vùng không có luật nào phủ — điều này có thể xảy ra với mẫu có tổ hợp giá trị hiếm gặp. Khi đó chương trình trả về `defaultClass` là lớp đa số trong tập train. Đây là dự đoán an toàn nhất khi không có thông tin.

---

## Giai đoạn 9 — Tính Accuracy Và Tổng Hợp

### Accuracy trên một fold
Sau khi dự đoán xong tất cả mẫu trong fold test hiện tại, em đếm số mẫu được dự đoán đúng và chia cho tổng số mẫu test. Đó là accuracy của fold đó.

### Accuracy trung bình 10 fold
Khi cả 10 fold đã được đánh giá, em lấy trung bình cộng của 10 giá trị accuracy. Đây là kết quả chính thức của thuật toán trên dataset đó. Ngoài ra em cũng tính độ lệch chuẩn giữa các fold để biết kết quả ổn định hay dao động lớn.

Ví dụ trên Iris, 10 fold cho ra accuracy lần lượt là 100%, 93%, 93%, 87%, 100%, 93%, 93%, 87%, 93%, 93%. Trung bình là 93.3%, độ lệch chuẩn khoảng 4.2%. Độ lệch chuẩn này phản ánh thực tế Iris là dataset nhỏ (chỉ 15 mẫu/fold), nên kết quả nhạy cảm với cách chia fold.

### Các thống kê đi kèm
Ngoài accuracy, em ghi lại thêm một số chỉ số để đánh giá hiệu năng và tính chất tập luật: tổng số luật mined trước khi tỉa, tổng số luật sau khi tỉa, thời gian huấn luyện tính bằng mili giây. Các con số này hữu ích khi so sánh các cấu hình tham số khác nhau.

---

## Giai đoạn 10 — Xuất Báo Cáo

### Báo cáo cho từng dataset
Với mỗi dataset, chương trình tự động sinh một file markdown trong thư mục `results/` tên dạng `<dataset>-report.md`. File này chứa: mô tả tóm tắt dataset (số mẫu, số thuộc tính, số lớp, phân bố lớp), accuracy trung bình kèm độ lệch chuẩn, accuracy từng fold riêng lẻ, số luật trước và sau tỉa, thời gian huấn luyện, và so sánh trực tiếp với accuracy công bố trong paper gốc.

### Báo cáo tổng hợp
File `results/ket-qua-trung-thuc.md` tổng hợp kết quả 26 dataset vào một bảng duy nhất: mỗi dòng một dataset, các cột gồm accuracy em đạt được, accuracy paper, chênh lệch, và trạng thái (thắng/thua/hoà). Ngoài bảng còn có thống kê tổng (bao nhiêu dataset khớp trong ±0.5%, bao nhiêu thắng, bao nhiêu thua), và phân tích chi tiết từng dataset có chênh lệch lớn kèm lý do suy đoán.

### Kết quả cuối cùng
Trung bình trên 26 dataset, cài đặt đạt **85.1%**, so với paper gốc công bố **85.2%** — chênh lệch chỉ **0.1%**, coi như khớp paper. Có **11 trên 26 dataset** đạt accuracy chênh với paper không quá 0.8%, trong đó Tic-Tac-Toe khớp chính xác tuyệt đối (99.2% ở cả hai bên). Các chênh lệch còn lại đều được phân tích và lý giải bằng các yếu tố khách quan như seed CV khác nhau, class imbalance trong dataset, hoặc chiến lược xử lý giá trị thiếu khác paper.

---

## Các Đảm Bảo Về Tính Trung Thực

Xuyên suốt mười giai đoạn trên, em duy trì các nguyên tắc sau để đảm bảo kết quả trung thực khoa học.

Thứ nhất, dữ liệu gốc không bao giờ bị sửa đổi trên đĩa — mọi biến đổi chỉ nằm trong bộ nhớ và bị xoá khi chương trình kết thúc. Thứ hai, rời rạc hoá MDL học cut points riêng cho từng fold CV, không có data leakage. Thứ ba, seed ngẫu nhiên cố định 42 giúp kết quả tái lập được. Thứ tư, em dùng stratified 10-fold CV — chuẩn benchmark trong ngành. Thứ năm, kết quả được so sánh trực tiếp với paper trên từng dataset riêng lẻ, không chỉ trung bình tổng. Thứ sáu, mỗi dataset có báo cáo riêng có thể kiểm chứng độc lập.

---

## Tham Chiếu Chéo

- Báo cáo chính: [BAO-CAO-CHINH-THUC.md](BAO-CAO-CHINH-THUC.md)
- Kết quả so sánh với paper: [ket-qua-trung-thuc.md](ket-qua-trung-thuc.md)
- Tổng quan thuật toán: [../TONG-QUAN-THUAT-TOAN.md](../TONG-QUAN-THUAT-TOAN.md)
- Review code: [code-review.md](code-review.md)
